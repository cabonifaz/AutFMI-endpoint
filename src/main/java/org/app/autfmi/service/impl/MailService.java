package org.app.autfmi.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.builders.ReportPDFBuilder;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.report.RequirementReport;
import org.app.autfmi.model.report.SolicitudEquipoReport;
import org.app.autfmi.service.IMailService;
import org.app.autfmi.util.MailUtils;
import org.app.autfmi.util.PDFUtils;
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
  private final PDFUtils pdfUtils;

  @Autowired
  private final ReportPDFBuilder reportPDFBuilder;

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
