package org.app.autfmi.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.RequirementDTO;
import org.app.autfmi.model.response.ParametrosListResponse;
import org.app.autfmi.repository.ParametrosRepository;
import org.app.autfmi.service.IMailService;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.MailUtils;
import org.app.autfmi.util.MasterDecoder;
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

  @Async
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
