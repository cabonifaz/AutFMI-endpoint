package org.app.autfmi.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.app.autfmi.model.dto.UserContactInfoDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.ParametrosDTO;
import org.app.autfmi.service.IParametrosService;
import org.app.autfmi.util.Constante;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.report.RequirementReport;
import org.app.autfmi.model.report.SolicitudEquipoReport;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.InterviewDetailResponseDTO;
import org.app.autfmi.service.IMailService;
import org.app.autfmi.util.MailUtils;
import org.app.autfmi.util.PDFUtils;
import org.app.autfmi.util.SafeValues;
import org.app.autfmi.util.builders.ReportPDFBuilder;
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
  private final PDFUtils pdfUtils;

  @Autowired
  private final ReportPDFBuilder reportPDFBuilder;

  @Autowired
  private IParametrosService parametrosService;

  private static final Logger logger = LoggerFactory.getLogger(MailService.class);

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
  @Async("notificationExecutor")
  @Override
  public void sendRequirementNotificationV2(RequirementReport report, String subject, List<String> toAddresses,
      List<String> ccAddresses,
      String action) {

    logger.info("Preparing to send requirement notification email...");
    logger.info("Asunto: {}", subject);

    // Limpiar duplicados en listas de emails
    List<String> cleanToAddresses = deduplicateEmailList(toAddresses);
    List<String> cleanCcAddresses = deduplicateEmailList(ccAddresses);

    // Log información de limpieza
    int originalToCount = toAddresses != null ? toAddresses.size() : 0;
    int originalCcCount = ccAddresses != null ? ccAddresses.size() : 0;

    logger.info("Emails TO procesados: {} originales -> {} limpios", originalToCount, cleanToAddresses.size());
    if (originalCcCount > 0) {
      logger.info("Emails CC procesados: {} originales -> {} limpios", originalCcCount, cleanCcAddresses.size());
    }

    if (originalToCount != cleanToAddresses.size()) {
      logger.warn("Se removieron {} emails TO duplicados o inválidos", originalToCount - cleanToAddresses.size());
    }
    if (originalCcCount != cleanCcAddresses.size()) {
      logger.warn("Se removieron {} emails CC duplicados o inválidos", originalCcCount - cleanCcAddresses.size());
    }

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

    // --- Vacantes con habilidades y carreras usando el nuevo formato ---
    List<Map<String, Object>> vacantesMapList = new ArrayList<>();

    if (report.getVacantesComplete() != null && !report.getVacantesComplete().isEmpty()) {
      for (var vacanteComplete : report.getVacantesComplete()) {
        if (vacanteComplete == null || vacanteComplete.getIdVacante() == null) {
          continue;
        }

        Map<String, Object> vacanteMap = new HashMap<>();
        vacanteMap.put("idPerfil", vacanteComplete.getIdVacante());
        vacanteMap.put("perfil", SafeValues.safeString(vacanteComplete.getPerfil()));
        vacanteMap.put("cantidad", vacanteComplete.getTotalVacantes() != null ? vacanteComplete.getTotalVacantes() : 1);

        // Mapear habilidades
        List<Map<String, Object>> habilidadesList = new ArrayList<>();
        if (vacanteComplete.getHabilidades() != null) {
          for (var habilidad : vacanteComplete.getHabilidades()) {
            if (habilidad != null) {
              Map<String, Object> skillMap = new HashMap<>();
              skillMap.put("habilidad", SafeValues.safeString(habilidad.getHabilidad()));
              skillMap.put("aniosExp", habilidad.getAExp() != null ? habilidad.getAExp() : 0);
              habilidadesList.add(skillMap);
            }
          }
        }
        vacanteMap.put("habilidades", habilidadesList);

        // Mapear carreras
        List<Map<String, Object>> carrerasList = new ArrayList<>();
        if (vacanteComplete.getCarreras() != null) {
          for (var carrera : vacanteComplete.getCarreras()) {
            if (carrera != null) {
              Map<String, Object> carreraMap = new HashMap<>();
              carreraMap.put("carrera", SafeValues.safeString(carrera.getCarrera()));
              carreraMap.put("grado", SafeValues.safeString(carrera.getGrado()));
              carrerasList.add(carreraMap);
            }
          }
        }
        vacanteMap.put("carreras", carrerasList);

        vacantesMapList.add(vacanteMap);
      }
    }

    variables.put("vacantes", vacantesMapList);

    // --- Postulantes ---
    List<Map<String, Object>> postulantesList = new ArrayList<>();
    if (report.getPostulants() != null && !report.getPostulants().isEmpty()) {
      for (var postulante : report.getPostulants()) {
        if (postulante == null || postulante.getNombreCompleto() == null) {
          continue; // Protección contra nulos
        }

        Map<String, Object> postulanteMap = new HashMap<>();

        // Dividir nombre completo de forma segura
        String nombreCompleto = SafeValues.safeString(postulante.getNombreCompleto()).trim();

        postulanteMap.put("nombres", nombreCompleto);
        postulanteMap.put("dni", postulante.getDni());
        postulanteMap.put("celular", SafeValues.safeString(postulante.getCelular()));
        postulanteMap.put("correo", SafeValues.safeString(postulante.getCorreo()));
        postulanteMap.put("perfil", SafeValues.safeString(postulante.getPerfil()));
        postulanteMap.put("estado", SafeValues.safeString(postulante.getEstado()));
        postulanteMap.put("situacion", postulante.getSituacion());

        postulantesList.add(postulanteMap);
      }
    }
    variables.put("postulantes", postulantesList.isEmpty() ? null : postulantesList);

    // Enviar correo a cada destinatario (usando listas limpias)
    for (String destinatario : cleanToAddresses) {
      logger.info("Enviando correo a: {}", destinatario);
      mailUtils.sendEmailWithHtmlTemplate(
          destinatario,
          cleanCcAddresses,
          subject,
          "rq-details",
          variables);
    }

    logger.info("Notificación V2 completada. Enviado a {} destinatarios.", cleanToAddresses.size());
  }

  /**
   * Elimina duplicados de una lista de emails manteniendo el orden original.
   * Normaliza emails a minúsculas para comparación pero mantiene formato
   * original.
   * Filtra emails nulos, vacíos y con formato inválido.
   * 
   * @param emailList Lista original de emails (puede ser null)
   * @return Lista limpia sin duplicados
   */
  private List<String> deduplicateEmailList(List<String> emailList) {
    List<String> cleanList = new ArrayList<>();

    if (emailList == null || emailList.isEmpty()) {
      return cleanList;
    }

    java.util.Set<String> seenEmails = new java.util.LinkedHashSet<>();

    for (String email : emailList) {
      if (email == null || email.trim().isEmpty()) {
        continue;
      }

      String trimmedEmail = email.trim();
      String normalizedEmail = trimmedEmail.toLowerCase();

      // Validación básica de formato email
      if (isValidEmailFormat(normalizedEmail) && seenEmails.add(normalizedEmail)) {
        cleanList.add(trimmedEmail);
      }
    }

    return cleanList;
  }

  /**
   * Validación básica de formato de email
   * 
   * @param email Email a validar (debe estar en minúsculas y sin espacios)
   * @return true si el formato es válido
   */
  private boolean isValidEmailFormat(String email) {
    // Validación simple: contiene @ y al menos un punto después del @
    return email != null &&
        email.contains("@") &&
        email.indexOf("@") > 0 &&
        email.indexOf("@") < email.length() - 1 &&
        email.lastIndexOf(".") > email.indexOf("@");
  }

  @Async("notificationExecutor")
  @Override
  public void sendCeseReportNotification(CeseReport report) {
    logger.info("Preparing to send cese report notification email...");

    String subject = "Cese de Empleado - " + report.getNombres() + " " + report.getApellidos();

    List<FileDTO> attachments = this.reportPDFBuilder
        .forCese(report)
        .withFormularioCese()
        .withDeactivateRequest()
        .build();

    String dest = report.getCorreoGestor();

    if (dest == null || dest.isEmpty()) {
      logger.error("Cese report email not sent: Gestor email is null or empty.");
      return;
    }

    try {
      pdfUtils.enviarCorreoConPDF(attachments, dest, new ArrayList<>(), subject,
          "Formulario de cese del empleado.");
      logger.info("Cese report email sent successfully to {}", dest);
    } catch (Exception e) {
      logger.error("Error sending cese report email: ", e);
    }
  }

  @Override
  public void sendMovementReportNotification(MovementReport report) {

    String fullname = report.getNombres() + " " + report.getApellidos();
    String subject = "Movimiento de Empleado - " + fullname;
    String dest = report.getCorreoGestor();

    List<FileDTO> attachments = this.reportPDFBuilder
        .forMovimiento(report)
        .withFormulario()
        .build();

    if (dest == null || dest.isEmpty()) {
      logger.error("Movement report email not sent: Gestor email is null or empty.");
      return;
    }

    try {
      String message = "Formulario de movimiento para el empleado: " + fullname;
      pdfUtils.enviarCorreoConPDF(
          attachments,
          dest,
          new ArrayList<>(),
          subject,
          message);
      logger.info("Movement report email sent successfully to {}", dest);
    } catch (Exception e) {
      logger.error("Error sending movement report email: ", e);
    }
  }

  @Override
  public void sendInterviewUnifiedNotification(
      InterviewDetailResponseDTO detail,
      String talentEmail,
      String talentFullName,
      BaseRequest actionUser,
      UserContactInfoDTO actionUserInfo,
      String actionType) {
    sendInterviewUnifiedNotification(detail, talentEmail, talentFullName, actionUser, actionUserInfo, actionType,
        null, null);
  }

  @Async("notificationExecutor")
  @Override
  public void sendInterviewUnifiedNotification(
      InterviewDetailResponseDTO detail,
      String talentEmail,
      String talentFullName,
      BaseRequest actionUser,
      UserContactInfoDTO actionUserInfo,
      String actionType,
      byte[] icsAttachment,
      String icsFileName) {

    if (detail == null || talentEmail == null || talentEmail.trim().isEmpty()) {
      logger.warn("Missing interview detail or talent email, skipping unified notification");
      return;
    }

    // CC: all registered interviewers with a valid email + the acting user
    List<String> ccList = new ArrayList<>();
    if (detail.getEntrevistadores() != null) {
      detail.getEntrevistadores().stream()
          .filter(e -> e.email() != null && !e.email().trim().isEmpty() && e.notificacion())
          .forEach(e -> ccList.add(e.email().trim()));
    }
    if (actionUserInfo != null && actionUserInfo.email() != null && !actionUserInfo.email().trim().isEmpty()) {
      ccList.add(actionUserInfo.email().trim());
    }

    System.out.println(ccList); 

    List<String> cleanCc = deduplicateEmailList(ccList);
    cleanCc.removeIf(cc -> cc.equalsIgnoreCase(talentEmail.trim()));

    String etapa = SafeValues.safeString(detail.getEtapa()).trim();

    // Strip "CODE - " prefix from perfil (e.g. "SOL_B412 - Desarrollador Backend" → "Desarrollador Backend")
    String perfilRaw = SafeValues.safeString(detail.getPerfil());
    String position = perfilRaw.contains(" - ")
        ? perfilRaw.substring(perfilRaw.indexOf(" - ") + 3)
        : perfilRaw;

    String allInterviewers = "";

    // All registered interviewer names, comma separated
    List<String> interviewerNames = new ArrayList<>();
    if (detail.getEntrevistadores() != null && !detail.getEntrevistadores().isEmpty()) {

      detail.getEntrevistadores().stream()
          .filter(e -> e.fullname() != null && !e.fullname().trim().isEmpty())
          .forEach(e -> interviewerNames.add(e.fullname().trim()));
      
      allInterviewers = String.join(", ", interviewerNames);

    } else { 

      allInterviewers = "Equipo de seleccion";

    } 

    String actionUserName = actionUserInfo != null && actionUserInfo.fullName() != null && !actionUserInfo.fullName().trim().isEmpty()
        ? actionUserInfo.fullName().trim()
        : (actionUser != null ? SafeValues.safeString(actionUser.getUsername()) : "Sistema");
    String actionUserPhone = actionUserInfo != null ? SafeValues.safeString(actionUserInfo.telefono()) : "";

    Map<String, Object> variables = new HashMap<>();
    variables.put("candidateName", SafeValues.safeString(talentFullName));
    variables.put("position", position);
    variables.put("formattedDate", formatSpanishDate(detail.getFecha()));
    variables.put("formattedTime", formatTime12h(detail.getHora()));
    variables.put("allInterviewers", allInterviewers);
    variables.put("clientName", SafeValues.safeString(detail.getClienteResumen()));
    variables.put("enlace", SafeValues.safeString(detail.getEnlaceEntrevista()));

    // Tipo de entrevista: la fuente de verdad es ID_TIPO_ENTREVISTA (maestro 47),
    // NO la presencia/ausencia de enlace, ubicación o dirección.
    // PRESENCIAL → dirección + ubicación (Google Maps); en cualquier otro caso
    // (VIRTUAL o no resoluble/legacy) se mantiene el enlace de la videollamada.
    String tipoEntrevista = resolveTipoEntrevista(detail.getIdTipoEntrevista());
    boolean esPresencial = Constante.TIPO_ENTREVISTA_PRESENCIAL.equalsIgnoreCase(tipoEntrevista);
    variables.put("esPresencial", esPresencial);
    variables.put("ubicacion", SafeValues.safeString(detail.getUbicacion()));
    variables.put("direccion", SafeValues.safeString(detail.getDireccion()));

    variables.put("accionUsuarioNombre", actionUserName);
    variables.put("accionUsuarioTelefono", actionUserPhone);

    String templateName;
    String subject;
    if ("Entrevista tecnica con líder técnico fractal".equalsIgnoreCase(etapa)) {
      templateName = "interview-tecnica-fractal";
      subject = "Entrevista Técnica - FRACTAL SOLUCIONES TI";
    } else if ("Entrevista técnica con cliente".equalsIgnoreCase(etapa)) {
      templateName = "interview-tecnica-cliente";
      subject = "Entrevista Técnica - FRACTAL SOLUCIONES TI";
    } else {
      templateName = "interview-general";
      subject = "¡Queremos conocerte! - FRACTAL SOLUCIONES TI";
    }

    // Inline images: header logo + signature assets
    Map<String, Resource> inlineResources = new LinkedHashMap<>();
    addInlineResourceIfPresent(inlineResources, "sig-logo", "assets/fractal-transparente.png");
    addInlineResourceIfPresent(inlineResources, "sig-mosaic", "assets/cubo-fractal.png");
    addInlineResourceIfPresent(inlineResources, "sig-linkedin", "assets/linkedin.png");
    addInlineResourceIfPresent(inlineResources, "sig-youtube", "assets/youtube.png");

    logger.info("Sending interview notification ({}) to: {} | CC: {} recipients | ICS: {}",
        templateName, talentEmail.trim(), cleanCc.size(),
        (icsAttachment != null && icsAttachment.length > 0));
    mailUtils.sendEmailWithHtmlTemplate(talentEmail.trim(), cleanCc, subject, templateName, variables,
        inlineResources, icsFileName, icsAttachment);
    logger.info("Interview notification dispatched.");
  }

  /**
   * Resuelve el texto del tipo de entrevista (p. ej. "PRESENCIAL" / "VIRTUAL") a
   * partir de ID_TIPO_ENTREVISTA, reutilizando el mecanismo centralizado de
   * parámetros (maestro 47). No hardcodea los IDs de los parámetros.
   *
   * @param idTipoEntrevista num1 del tipo de entrevista (o null).
   * @return el string1 del parámetro correspondiente, o "" si no se resuelve.
   */
  private String resolveTipoEntrevista(Integer idTipoEntrevista) {
    if (idTipoEntrevista == null) {
      return "";
    }
    try {
      var response = parametrosService.listParametros(Constante.TIPO_ENTREVISTA);
      if (response == null || response.getListParametros() == null) {
        return "";
      }
      return response.getListParametros().stream()
          .filter(p -> idTipoEntrevista.equals(p.getNum1()))
          .map(ParametrosDTO::getString1)
          .filter(s -> s != null)
          .findFirst()
          .orElse("");
    } catch (Exception e) {
      logger.warn("No se pudo resolver el tipo de entrevista (id={}): {}", idTipoEntrevista, e.getMessage());
      return "";
    }
  }

  private void addInlineResourceIfPresent(Map<String, Resource> inlineResources, String cid, String classpath) {
    try {
      ClassPathResource resource = new ClassPathResource(classpath);
      if (resource.exists()) {
        inlineResources.put(cid, resource);
      }
    } catch (Exception e) {
      logger.warn("Resource '{}' not available, sending without it: {}", classpath, e.getMessage());
    }
  }

  private String formatSpanishDate(String fechaStr) {
    if (fechaStr == null || fechaStr.trim().isEmpty()) return "";
    try {
      LocalDate date;
      String trimmed = fechaStr.trim();
      if (trimmed.length() == 10 && trimmed.charAt(4) == '-') {
        date = LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      } else if (trimmed.contains("/")) {
        date = LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
      } else {
        return fechaStr;
      }
      String formattedDate = date.format(
          DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-PE")));
      return Character.toUpperCase(formattedDate.charAt(0)) + formattedDate.substring(1);
    } catch (Exception e) {
      logger.warn("Could not format date '{}': {}", fechaStr, e.getMessage());
      return fechaStr;
    }
  }

  private String formatTime12h(String horaStr) {
    if (horaStr == null || horaStr.trim().isEmpty()) return "";
    try {
      LocalTime time;
      String trimmed = horaStr.trim();
      if (trimmed.length() == 8) {
        time = LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("HH:mm:ss"));
      } else if (trimmed.length() == 5) {
        time = LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("HH:mm"));
      } else {
        return horaStr;
      }
      return time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US));
    } catch (Exception e) {
      logger.warn("Could not format time '{}': {}", horaStr, e.getMessage());
      return horaStr;
    }
  }

  @Async("notificationExecutor")
  @Override
  public void sendEquipmentRequestNotification(SolicitudEquipoReport report) {

    String employee = report.getNombreEmpleado() + " " + report.getApellidosEmpleado();
    String subject = "Requerimiento de Software y Hardware - " + employee;
    String message = "Solicitud de equipo para: " + employee;

    List<FileDTO> attachments = this.reportPDFBuilder
        .fEquipoReport(report)
        .withFormulario()
        .build();

    String dest = report.getCorreoGestor();

    if (dest == null || dest.isEmpty())
      throw new IllegalArgumentException("Correo de gestor no pude ser nulo o vacío");

    try {
      pdfUtils.enviarCorreoConPDF(attachments, dest, new ArrayList<>(), subject,
          message);
      logger.info("Mail sent to: {}", dest);
    } catch (Exception e) {
      logger.error("Error sending report mail: {}", e);
    }

  }
}
