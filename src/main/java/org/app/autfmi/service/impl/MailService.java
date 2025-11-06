package org.app.autfmi.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.RequirementDTO;
import org.app.autfmi.model.report.RequirementReport;
import org.app.autfmi.model.response.ParametrosListResponse;
import org.app.autfmi.repository.ParametrosRepository;
import org.app.autfmi.service.IMailService;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.MailUtils;
import org.app.autfmi.util.MasterDecoder;
import org.app.autfmi.util.SafeValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService implements IMailService {

  @Autowired
  private MailUtils mailUtils;

  @Autowired
  private ParametrosRepository paramsRepository;

  private static final Logger logger = LoggerFactory.getLogger(MailService.class);

  @Async
  @Override
  public void sendCreateRequirementNotification(String userName, RequirementDTO rDto,
      List<Map<String, Object>> vacantesMapped,
      List<Map<String, Object>> contactosMapList,
      List<Map<String, Object>> habilidadesMapped,
      List<Map<String, Object>> carrerasMapped,
      String correoEjecutor) {

    logger.info("Preparing to send create requirement notification email...");
    logger.info("Asunto: Creación de Requerimiento: " + rDto.getCodigoRQ());

    Map<String, Object> variables = new HashMap<>();

    // Auditoría de acción
    Map<String, Object> accion = new HashMap<>();
    accion.put("tipo", "Creación de Requerimiento");
    accion.put("usuario", userName);
    accion.put("fechaHora", java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Lima"))
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withLocale(new java.util.Locale("es", "PE"))));

    variables.put("accion", accion);

    // --- Datos del requerimiento ---
    Map<String, Object> rqData = new HashMap<>();
    rqData.put("codigo", rDto.getCodigoRQ());
    rqData.put("titulo", rDto.getTitulo());
    rqData.put("descripcion", rDto.getDescripcion());
    rqData.put("fechaSolicitud", rDto.getFechaSolicitud());
    rqData.put("fechaVencimiento", rDto.getFechaVencimiento());

    // Estado del requerimiento
    Integer idEstado = rDto.getIdEstado();
    String decodedState = MasterDecoder.decodeRQState(idEstado);
    rqData.put("estado", decodedState);
    variables.put("rqData", rqData);

    // --- Datos de gestión ---
    Map<String, Object> gestion = new HashMap<>();

    if (rDto.getDuracion() == null || rDto.getIdDuracion() == null) {
      logger.warn("RQ no tiene duración: {}", rDto.getCodigoRQ());
      gestion.put("duracion", "Sin duración especificada");
    } else {
      Integer duration = BigDecimal.valueOf(rDto.getDuracion() != null ? rDto.getDuracion().doubleValue() : 0)
          .intValue();

      // -- Gestion duracion
      Integer idDuration = rDto.getIdDuracion();
      String decodedDuration = MasterDecoder.decodeDuration(idDuration);
      gestion.put("duracion", duration + " " + decodedDuration);
    }

    // -- Gestion modalidad
    Integer idModalidad = rDto.getIdModalidad();
    String decodedModalidad = MasterDecoder.decodeContractType(idModalidad);
    gestion.put("modalidad", decodedModalidad);

    // -- Gestion modalidades facturación
    String modalidadFact = rDto.getModalidadFact();
    List<String> decodedFact = MasterDecoder.decodeModalityFacturationList(modalidadFact);

    gestion.put("modalidadesFacturacion", decodedFact);
    variables.put("gestion", gestion);

    // --- Cliente y contactos ---
    Map<String, Object> cliente = new HashMap<>();
    cliente.put("nombre", rDto.getCliente());
    cliente.put("contactos", contactosMapList);
    variables.put("cliente", cliente);

    // --- Vacantes ---
    // --- Vacantes + habilidades + carreras ---
    for (Map<String, Object> vacante : vacantesMapped) {
      Integer idVacante = (Integer) vacante.get("idPerfil");

      // Filtrar habilidades técnicas para esta vacante
      List<Map<String, Object>> habilidadesList = new ArrayList<>();
      if (habilidadesMapped != null && !habilidadesMapped.isEmpty()) {
        habilidadesMapped.stream()
            .filter(h -> idVacante.equals(h.get("ID_REQUERIMIENTO_VACANTE")))
            .forEach(h -> {
              Map<String, Object> skillMap = new HashMap<>();
              skillMap.put("habilidad", h.get("NOMBRE_HABILIDAD"));
              skillMap.put("aniosExp", h.get("ANIOS_EXPERIENCIA"));
              habilidadesList.add(skillMap);
            });
      }

      // Filtrar carreras para esta vacante
      List<Map<String, Object>> carrerasList = new ArrayList<>();
      if (carrerasMapped != null && !carrerasMapped.isEmpty()) {
        carrerasMapped.stream()
            .filter(c -> idVacante.equals(c.get("ID_VACANTE")))
            .forEach(c -> {
              Map<String, Object> carreraMap = new HashMap<>();
              carreraMap.put("carrera", c.get("CARRERA"));
              carreraMap.put("grado", c.get("GRADO_ESTUDIO"));
              carrerasList.add(carreraMap);
            });
      }

      vacante.put("habilidades", habilidadesList);
      vacante.put("carreras", carrerasList);
    }

    variables.put("vacantes", vacantesMapped);

    // --- Postulantes ---
    // Lista vacía porque al crearse el requerimiento no hay postulantes aún
    variables.put("postulantes", null);

    // Obtener destinatarios desde base de datos
    List<String> destinatarios = getDestinatariosFromParams();
    logger.info("Enviando notificación a los siguientes destinatarios: " + Arrays.toString(destinatarios.toArray()));

    for (String destinatario : destinatarios) {
      mailUtils.sendEmailWithHtmlTemplate(
          destinatario,
          null,
          "Creación de Requerimiento: " + rDto.getCodigoRQ(),
          "rq-details",
          variables);
    }

    // Enviar copia al ejecutor si su correo está disponible
    if (correoEjecutor != null && !correoEjecutor.isEmpty()) {
      logger.info("Enviando copia al ejecutor del requerimiento: " + correoEjecutor);
      mailUtils.sendEmailWithHtmlTemplate(
          correoEjecutor,
          null,
          "Creación de Requerimiento: " + rDto.getCodigoRQ(),
          "rq-details",
          variables);
    }

    logger.info("Notificación completada para creación de requerimiento.");
  }

  /**
   * Enviar notificación de una acción sobre requerimiento
   * Revisar la estructura de datos que retorna el SP SP_REQUERIMIENTO_INS_SMART
   * Revisar como se mapea el report en
   * RequirementRepository.saveRequirementByAgent
   * 
   * @param report      El objeto RequirementReport que contiene todos los datos
   *                    del RQ.
   * @param subject     El asunto del correo electrónico.
   * @param toAddresses Lista de direcciones de correo electrónico de los
   *                    destinatarios.
   * @param ccAddresses Lista de direcciones de correo electrónico para copia.
   * @param action      La acción que se está realizando (por ejemplo,
   *                    "CREAR_EDITAR_REQUERIMIENTO_AGENTE").
   */
  @Async
  @Override
  public void sendRequirementNotificationV2(RequirementReport report, String subject, List<String> toAddresses,
      List<String> ccAddresses,
      String action) {

    logger.info("Preparing to send requirement notification email...");
    logger.info("Asunto: {}", subject);

    Map<String, Object> variables = new HashMap<>();

    // Auditoría de acción (usando datos del usuario que realizó la acción)
    Map<String, Object> accion = new HashMap<>();
    if (report.getActionUser() != null) {
      accion.put("tipo", action);
      accion.put("usuario", report.getActionUser().getUsuario());
      accion.put("fechaHora", java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Lima"))
          .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
              .withLocale(new java.util.Locale("es", "PE"))));
    } else {
      accion.put("tipo", "Creación de Requerimiento");
      accion.put("usuario", "Sistema");
      accion.put("fechaHora", java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Lima"))
          .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
              .withLocale(new java.util.Locale("es", "PE"))));
    }
    variables.put("accion", accion);

    // --- Datos del requerimiento ---
    Map<String, Object> rqData = new HashMap<>();
    if (report.getRequirementDetails() != null) {
      rqData.put("codigo", report.getRequirementDetails().getCodigoRQ());
      rqData.put("titulo", report.getRequirementDetails().getTitulo());
      rqData.put("descripcion", report.getRequirementDetails().getDescripcion());
      rqData.put("fechaSolicitud", report.getRequirementDetails().getFechaSolicitud());
      rqData.put("fechaVencimiento", report.getRequirementDetails().getFechaVencimiento());
      rqData.put("estado", report.getRequirementDetails().getEstadoRQ());
    }
    variables.put("rqData", rqData);

    // --- Datos de gestión ---
    Map<String, Object> gestion = new HashMap<>();
    if (report.getRequirementDetails() != null) {
      gestion.put("duracion", report.getRequirementDetails().getDuracionRQ());
      gestion.put("modalidad", report.getRequirementDetails().getModalidadRQ());
      gestion.put("modalidadesFacturacion", new ArrayList<>());
    }
    variables.put("gestion", gestion);

    // --- Cliente y contactos ---
    Map<String, Object> cliente = new HashMap<>();
    cliente.put("nombre", SafeValues.safeString(report.getRequirementDetails().getCliente()));

    List<Map<String, Object>> contactosMapList = new ArrayList<>();
    if (report.getContacts() != null) {
      for (var contacto : report.getContacts()) {
        Map<String, Object> contactoMap = new HashMap<>();
        contactoMap.put("nombre", contacto.getNombreCompleto());
        contactoMap.put("celular", contacto.getTelefono());
        contactoMap.put("correo", contacto.getCorreo());
        contactoMap.put("cargo", contacto.getCargo());
        contactosMapList.add(contactoMap);
      }
    }
    cliente.put("contactos", contactosMapList);
    variables.put("cliente", cliente);

    // --- Vacantes con habilidades y carreras ---
    List<Map<String, Object>> vacantesMapList = new ArrayList<>();

    // Agrupar habilidades por vacante
    Map<Integer, List<Map<String, Object>>> habilidadesPorVacante = new HashMap<>();
    if (report.getVacanteSkills() != null) {
      for (var skill : report.getVacanteSkills()) {
        habilidadesPorVacante.computeIfAbsent(skill.getIdVacante(), k -> new ArrayList<>())
            .add(Map.of("habilidad", skill.getHabilidad(), "aniosExp", skill.getAExp()));
      }
    }

    // Agrupar carreras por vacante
    Map<Integer, List<Map<String, Object>>> carrerasPorVacante = new HashMap<>();
    if (report.getVacanteCareers() != null) {
      for (var career : report.getVacanteCareers()) {
        carrerasPorVacante.computeIfAbsent(career.getIdVacante(), k -> new ArrayList<>())
            .add(Map.of("carrera", SafeValues.safeString(career.getCarrera()), "grado",
                SafeValues.safeString(career.getGrado())));
      }
    }

    // Crear lista de vacantes únicas
    if (report.getVacanteSkills() != null || report.getVacanteCareers() != null) {
      // Obtener IDs únicos de vacantes
      java.util.Set<Integer> idsVacantes = new java.util.HashSet<>();
      if (report.getVacanteSkills() != null) {
        report.getVacanteSkills().forEach(s -> idsVacantes.add(s.getIdVacante()));
      }
      if (report.getVacanteCareers() != null) {
        report.getVacanteCareers().forEach(c -> idsVacantes.add(c.getIdVacante()));
      }

      for (Integer idVacante : idsVacantes) {
        Map<String, Object> vacanteMap = new HashMap<>();
        vacanteMap.put("idPerfil", idVacante);

        // Obtener perfil desde habilidades (si existe)
        String perfil = "Perfil Profesional";
        if (report.getVacanteSkills() != null) {
          var skillOpt = report.getVacanteSkills().stream()
              .filter(s -> s.getIdVacante().equals(idVacante))
              .findFirst();
          if (skillOpt.isPresent()) {
            perfil = skillOpt.get().getPerfil();
          }
        }

        vacanteMap.put("perfil", perfil);
        vacanteMap.put("cantidad", 1); // Default
        vacanteMap.put("habilidades", habilidadesPorVacante.getOrDefault(idVacante, new ArrayList<>()));
        vacanteMap.put("carreras", carrerasPorVacante.getOrDefault(idVacante, new ArrayList<>()));

        vacantesMapList.add(vacanteMap);
      }
    }
    variables.put("vacantes", vacantesMapList);

    // --- Postulantes ---
    List<Map<String, Object>> postulantesList = new ArrayList<>();
    if (report.getPostulants() != null) {
      for (var postulante : report.getPostulants()) {
        Map<String, Object> postulanteMap = new HashMap<>();
        postulanteMap.put("nombres", postulante.getNombreCompleto().split(" ")[0]);
        postulanteMap.put("apellidos", postulante.getNombreCompleto().substring(
            postulante.getNombreCompleto().indexOf(" ") + 1));
        postulanteMap.put("dni", "N/A");
        postulanteMap.put("celular", postulante.getCelular());
        postulanteMap.put("correo", postulante.getCorreo());
        postulanteMap.put("perfil", postulante.getPerfil());
        postulanteMap.put("estado", postulante.getEstado());
        postulanteMap.put("situacion", "N/A");
        postulantesList.add(postulanteMap);
      }
    }
    variables.put("postulantes", postulantesList);

    // Enviar correo a cada destinatario
    for (String destinatario : toAddresses) {
      logger.info("Enviando correo a: {}", destinatario);
      mailUtils.sendEmailWithHtmlTemplate(
          destinatario,
          ccAddresses,
          subject,
          "rq-details",
          variables);
    }

    logger.info("Notificación V2 completada. Enviado a {} destinatarios.", toAddresses.size());
  }

  /**
   * @deprecated Use
   *             {@link #sendRequirementNotificationV2(RequirementReport, String, List, List)}
   *             instead.
   */
  @Async
  @Deprecated
  @Override
  public void sendUpdateRequirementNotification(String userName, RequirementDTO rDto,
      List<Map<String, Object>> vacantesMapped, List<Map<String, Object>> contactosMapList,
      List<Map<String, Object>> habilidadesMapped, List<Map<String, Object>> carrerasMapped,
      List<Map<String, Object>> postulanteList,
      String correoEjecutor) {

    logger.info("Preparing to send update requirement notification email...");
    logger.info("Asunto: Actualización de Requerimiento: " + rDto.getCodigoRQ());

    Map<String, Object> variables = new HashMap<>();

    // Auditoría de acción
    Map<String, Object> accion = new HashMap<>();
    accion.put("tipo", "Actualización de Requerimiento");
    accion.put("usuario", userName);
    accion.put("fechaHora", java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Lima"))
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withLocale(new java.util.Locale("es", "PE"))));

    variables.put("accion", accion);

    // --- Datos del requerimiento ---
    Map<String, Object> rqData = new HashMap<>();
    rqData.put("codigo", rDto.getCodigoRQ());
    rqData.put("titulo", rDto.getTitulo());
    rqData.put("descripcion", rDto.getDescripcion());
    rqData.put("fechaSolicitud", rDto.getFechaSolicitud());
    rqData.put("fechaVencimiento", rDto.getFechaVencimiento());

    // Estado del requerimiento
    Integer idEstado = rDto.getIdEstado();
    String decodedState = MasterDecoder.decodeRQState(idEstado);
    rqData.put("estado", decodedState);
    variables.put("rqData", rqData);

    // --- Datos de gestión ---
    Map<String, Object> gestion = new HashMap<>();

    // -- Gestion duracion
    if (rDto.getDuracion() == null || rDto.getIdDuracion() == null) {
      logger.warn("RQ no tiene duración: {}", rDto.getCodigoRQ());
      gestion.put("duracion", "Sin duración especificada");
    } else {
      Integer duration = BigDecimal.valueOf(rDto.getDuracion() != null ? rDto.getDuracion().doubleValue() : 0)
          .intValue();

      // -- Gestion duracion
      Integer idDuration = rDto.getIdDuracion();
      String decodedDuration = MasterDecoder.decodeDuration(idDuration);
      gestion.put("duracion", duration + " " + decodedDuration);
    }

    // -- Gestion modalidad
    Integer idModalidad = rDto.getIdModalidad();
    String decodedModalidad = MasterDecoder.decodeContractType(idModalidad);
    gestion.put("modalidad", decodedModalidad);

    // -- Gestion modalidades facturación
    String modalidadFact = rDto.getModalidadFact();
    List<String> decodedFact = MasterDecoder.decodeModalityFacturationList(modalidadFact);

    gestion.put("modalidadesFacturacion", decodedFact);
    variables.put("gestion", gestion);

    // --- Cliente y contactos ---
    Map<String, Object> cliente = new HashMap<>();
    cliente.put("nombre", rDto.getCliente());
    cliente.put("contactos", contactosMapList);
    variables.put("cliente", cliente);

    // --- Vacantes + habilidades + carreras ---
    for (Map<String, Object> vacante : vacantesMapped) {
      Integer idVacante = (Integer) vacante.get("idPerfil");

      // Filtrar habilidades técnicas para esta vacante
      List<Map<String, Object>> habilidadesList = new ArrayList<>();
      if (habilidadesMapped != null && !habilidadesMapped.isEmpty()) {
        habilidadesMapped.stream()
            .filter(h -> idVacante.equals(h.get("ID_REQUERIMIENTO_VACANTE")))
            .forEach(h -> {
              Map<String, Object> skillMap = new HashMap<>();
              skillMap.put("habilidad", h.get("NOMBRE_HABILIDAD"));
              skillMap.put("aniosExp", h.get("ANIOS_EXPERIENCIA"));
              habilidadesList.add(skillMap);
            });
      }

      // Filtrar carreras para esta vacante
      List<Map<String, Object>> carrerasList = new ArrayList<>();
      if (carrerasMapped != null && !carrerasMapped.isEmpty()) {
        carrerasMapped.stream()
            .filter(c -> idVacante.equals(c.get("ID_VACANTE")))
            .forEach(c -> {
              Map<String, Object> carreraMap = new HashMap<>();
              carreraMap.put("carrera", c.get("CARRERA"));
              carreraMap.put("grado", c.get("GRADO_ESTUDIO"));
              carrerasList.add(carreraMap);
            });
      }

      vacante.put("habilidades", habilidadesList);
      vacante.put("carreras", carrerasList);
    }

    variables.put("vacantes", vacantesMapped);

    // --- Postulantes ---
    // Para actualización de requerimiento, incluimos los postulantes existentes
    variables.put("postulantes", postulanteList);

    // Obtener destinatarios desde base de datos
    // Obtener destinatarios desde base de datos
    List<String> destinatarios = getDestinatariosFromParams();
    logger.info("Enviando notificación a los siguientes destinatarios: " + Arrays.toString(destinatarios.toArray()));

    logger.info("Starting mail utils for update");

    for (String destinatario : destinatarios) {
      mailUtils.sendEmailWithHtmlTemplate(
          destinatario,
          null,
          "Actualización de Requerimiento: " + rDto.getCodigoRQ(),
          "rq-details",
          variables);
    }

    // Enviar copia al ejecutor si su correo está disponible
    if (correoEjecutor != null && !correoEjecutor.isEmpty()) {
      logger.info("Enviando copia al ejecutor del requerimiento: " + correoEjecutor);
      mailUtils.sendEmailWithHtmlTemplate(
          correoEjecutor,
          null,
          "Actualización de Requerimiento: " + rDto.getCodigoRQ(),
          "rq-details",
          variables);
    }

    logger.info("Finished mail utils for update.");
  }

  private List<String> getDestinatariosFromParams() {
    logger.info("=== Iniciando getDestinatariosFromParams ===");

    if (paramsRepository == null) {
      logger.error("paramsRepository es NULL - no se puede obtener destinatarios desde BD");
      return Arrays.asList("jean.velasquez@fractalservicios.pe"); // fallback
    }

    logger.info("paramsRepository inicializado correctamente");

    try {
      logger.info("Llamando a paramsRepository.listParametros con constante: {}", Constante.NOTIFICACION_RQ_EMAILS);
      ParametrosListResponse paramsResponse = paramsRepository.listParametros(Constante.NOTIFICACION_RQ_EMAILS);
      List<String> emails = new ArrayList<>();

      if (paramsResponse != null) {
        logger.info("Respuesta de parámetros recibida - Tipo mensaje: {}",
            paramsResponse.getBaseResponse().getIdTipoMensaje());

        if (paramsResponse.getBaseResponse().getIdTipoMensaje() == 2) {
          logger.info("Parámetros encontrados: {}", paramsResponse.getListParametros().size());

          for (var parametro : paramsResponse.getListParametros()) {
            if (parametro.getString1() != null && !parametro.getString1().isEmpty()) {
              logger.info("Agregando email: {}", parametro.getString1());
              emails.add(parametro.getString1());
            }
          }
        } else {
          logger.warn("No se encontraron parámetros válidos - Mensaje: {}",
              paramsResponse.getBaseResponse().getMensaje());
        }
      } else {
        logger.error("paramsResponse es NULL");
      }

      if (emails.isEmpty()) {
        logger.warn("No se encontraron emails en parámetros, usando email por defecto");
        emails.add("jean.velasquez@fractalservicios.pe");
      }

      logger.info("Emails finales obtenidos: {}", emails.toString());
      return emails;

    } catch (Exception e) {
      logger.error("Error al obtener destinatarios desde parámetros: {}", e.getMessage(), e);
      return Arrays.asList("jean.velasquez@fractalservicios.pe"); // fallback
    }
  }

}
