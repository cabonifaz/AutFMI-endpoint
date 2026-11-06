package org.app.autfmi.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.InicioOutsourcingDTO;
import org.app.autfmi.model.dto.ContactoTalentoDTO;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.ParametrosDTO;
import org.app.autfmi.repository.EmployeeRepository;
import org.app.autfmi.service.IParametrosService;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.EventoNotificacion;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.report.RequirementReport;
import org.app.autfmi.model.report.SolicitudEquipoReport;
import org.app.autfmi.service.IMailService;
import org.app.autfmi.util.FormularioCorreoUtils;
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

  @Autowired
  private EmployeeRepository employeeRepository;

  @Autowired
  private NotificacionCorreoSwitch correoSwitch;

  private static final Logger logger = LoggerFactory.getLogger(MailService.class);

  @Async("notificationExecutor")
  @Override
  public void sendRequirementNotificationV2(EventoNotificacion evento, RequirementReport report, String subject,
      List<String> toAddresses, List<String> ccAddresses, String action) {

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
      gestion.put("duracion", SafeValues.safeString(report.getRequirementDetails().getDuracionRQ()));
      gestion.put("duracionContrato",
          SafeValues.safeString(report.getRequirementDetails().getDuracionContrato()));
      gestion.put("modalidad", report.getRequirementDetails().getModalidadRQ());
      gestion.put("modalidadesFacturacion", new ArrayList<>());
      // Requisitos de ingreso (EMO, SCTR, asignación de laptop)
      gestion.put("requisitos", FormularioCorreoUtils.requisitosIngreso(
          report.getRequirementDetails().getEmo(),
          report.getRequirementDetails().getSctr(),
          report.getRequirementDetails().getAsignacionLaptop()));
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
          evento,
          destinatario,
          cleanCcAddresses,
          subject,
          "rq-details",
          variables);
    }

    // Cada envío es asíncrono y registra por su cuenta si salió o se omitió.
    logger.info("Notificación V2 encolada para {} destinatarios.", cleanToAddresses.size());
  }

  
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

  private boolean isValidEmailFormat(String email) {
    // Validación simple: contiene @ y al menos un punto después del @
    return email != null &&
        email.contains("@") &&
        email.indexOf("@") > 0 &&
        email.indexOf("@") < email.length() - 1 &&
        email.lastIndexOf(".") > email.indexOf("@");
  }

  
  private List<String> resolveMaestroEmails(String maestro) {
    List<String> emails = new ArrayList<>();
    try {
      var response = parametrosService.listParametros(maestro);
      if (response == null || response.getListParametros() == null) {
        return emails;
      }
      for (ParametrosDTO p : response.getListParametros()) {
        String s = p.getString1();
        if (s != null && !s.trim().isEmpty()) {
          emails.add(s.trim());
        }
      }
    } catch (Exception e) {
      logger.warn("No se pudo resolver los correos del maestro {}: {}", maestro, e.getMessage());
    }
    return emails;
  }

  // Separa una lista de correos separados por coma en una lista limpia. 
  private List<String> splitEmails(String csv) {
    List<String> list = new ArrayList<>();
    if (csv == null || csv.trim().isEmpty()) {
      return list;
    }
    for (String e : csv.split(",")) {
      if (e != null && !e.trim().isEmpty()) {
        list.add(e.trim());
      }
    }
    return list;
  }

  
  private List<String> buildFormularioCcList(String correoGestor, String gestoresCsv, List<String> extraToEmails) {
    List<String> cc = new ArrayList<>();
    if (correoGestor != null && !correoGestor.trim().isEmpty()) {
      cc.add(correoGestor.trim());
    }
    cc.addAll(resolveMaestroEmails(Constante.MAESTRO_CORREO_SELECCION));
    cc.addAll(splitEmails(gestoresCsv));
    if (extraToEmails != null) {
      cc.addAll(extraToEmails);
    }
    return deduplicateEmailList(cc);
  }

  
  private boolean sendFormularioEmail(EventoNotificacion evento, List<FileDTO> files, String maestro,
      String correoGestor, String gestoresCsv, String subject, String message) {
    return sendFormularioEmail(evento, files, maestro, correoGestor, gestoresCsv, subject, message, false);
  }

  /** Igual que el anterior; co {@code html = true} el cuerpo va en HTML. */
  private boolean sendFormularioEmail(EventoNotificacion evento, List<FileDTO> files, String maestro,
      String correoGestor, String gestoresCsv, String subject, String message, boolean html) {
    List<String> toEmails = resolveMaestroEmails(maestro);
    String to = !toEmails.isEmpty()
        ? toEmails.get(0)
        : (correoGestor != null ? correoGestor.trim() : "");
    if (to == null || to.isEmpty()) {
      logger.error("Correo '{}' no enviado: sin destinatario (maestro {} ni correoGestor).", subject, maestro);
      return false;
    }
    List<String> extraTo = toEmails.size() > 1
        ? new ArrayList<>(toEmails.subList(1, toEmails.size()))
        : new ArrayList<>();
    List<String> cc = buildFormularioCcList(correoGestor, gestoresCsv, extraTo);
    try {
      logger.info("Enviando '{}' -> TO: {} | CC: {}", subject, to, cc);
      return pdfUtils.enviarCorreoConPDF(evento, files, to, cc, subject, message, html);
    } catch (Exception e) {
      logger.error("Error enviando '{}': ", subject, e);
      return false;
    }
  }

  private String buildFormularioSubject(String accion, String cliente, String nombre) {
    return "Solicitud de " + accion + ": " + SafeValues.safeString(cliente) + " - " + SafeValues.safeString(nombre);
  }

  private String buildFormularioBody(String accion) {
    return "Estimados,\n\nFavor atender la solicitud de " + accion + ".\nSlds";
  }

  /**
   * Envía los correos del ingreso: (1) Formulario de Ingreso al maestro 52 y
   * (2) Solicitud de Creación de Usuario al maestro 51.
   */
  @Async("notificationExecutor")
  @Override
  public void sendEntryReportNotification(EntryReport report, Integer idTalento) {
    sendEntryReportNotificationSync(report, idTalento);
  }

  @Override
  public boolean sendEntryReportNotificationSync(EntryReport report, Integer idTalento) {
    // Una sola consulta para los dos correos; sin permiso no se generan los PDFs.
    try (var decision = correoSwitch.decision(EventoNotificacion.INGRESO)) {
      return decision.habilitado() && enviarFormulariosIngreso(report, idTalento);
    }
  }

  private boolean enviarFormulariosIngreso(EntryReport report, Integer idTalento) {
    String fullname = report.getNombres() + " " + report.getApellidos();
    String cliente = report.getCliente();
    String gestores = report.getGestoresCliente();

    // Correo 1: Formulario de Ingreso -> maestro 52 (talento)
    boolean ingreso = sendFormularioEmail(EventoNotificacion.INGRESO,
        this.reportPDFBuilder.forIngreso(report).withFormulario().build(),
        Constante.MAESTRO_CORREO_TALENTO, report.getCorreoGestor(), gestores,
        buildFormularioSubject("ingreso", cliente, fullname),
        buildFormularioBody("ingreso"));

    // Correo 2: Solicitud de Creación de Usuario -> maestro 51 (soporte), con la
    // tabla de contacto del talento en el cuerpo.
    var contacto = contactoTalento(idTalento, report.getNombres(), report.getApellidos(), null);
    boolean usuario = sendFormularioEmail(EventoNotificacion.INGRESO,
        this.reportPDFBuilder.forIngreso(report).withCreateUser().build(),
        Constante.MAESTRO_CORREO_SOPORTE, report.getCorreoGestor(), gestores,
        buildFormularioSubject("creación de usuario", cliente, fullname),
        FormularioCorreoUtils.cuerpoSolicitudHtml("creación de usuario", List.of(contacto)), true);

    return ingreso && usuario;
  }

  @Async("notificationExecutor")
  @Override
  public void sendCeseReportNotification(CeseReport report) {
    sendCeseReportNotificationSync(report);
  }

  @Override
  public boolean sendCeseReportNotificationSync(CeseReport report) {
    try (var decision = correoSwitch.decision(EventoNotificacion.CESE)) {
      if (!decision.habilitado()) {
        return false;
      }
      logger.info("Preparing to send cese report notification email...");
      String fullname = report.getNombres() + " " + report.getApellidos();

      List<FileDTO> attachments = this.reportPDFBuilder
          .forCese(report)
          .withFormularioCese()
          .withDeactivateRequest()
          .build();

      // TO: maestro 52 (talento). CC: generador + selección(35) + gestores del cliente.
      return sendFormularioEmail(EventoNotificacion.CESE, attachments, Constante.MAESTRO_CORREO_TALENTO,
          report.getCorreoGestor(), report.getGestoresCliente(),
          buildFormularioSubject("cese", report.getCliente(), fullname),
          buildFormularioBody("cese"));
    }
  }

  @Override
  public void sendMovementReportNotification(MovementReport report) {
    sendMovementReportNotificationSync(report);
  }

  @Override
  public boolean sendMovementReportNotificationSync(MovementReport report) {
    try (var decision = correoSwitch.decision(EventoNotificacion.MOVIMIENTO)) {
      if (!decision.habilitado()) {
        return false;
      }
      String fullname = report.getNombres() + " " + report.getApellidos();

      List<FileDTO> attachments = this.reportPDFBuilder
          .forMovimiento(report)
          .withFormulario()
          .build();

      // TO: maestro 52 (talento). CC: generador + selección(35) + gestores del cliente.
      return sendFormularioEmail(EventoNotificacion.MOVIMIENTO, attachments, Constante.MAESTRO_CORREO_TALENTO,
          report.getCorreoGestor(), report.getGestoresCliente(),
          buildFormularioSubject("movimiento", report.getCliente(), fullname),
          buildFormularioBody("movimiento"));
    }
  }

  /**
   * Notificación de próximo inicio de labores de un talento en outsourcing.
   * TO = contactos del requerimiento; CC = gestores del cliente + Selección(35).
   * Es SÍNCRONO a propósito: el job solo marca el contrato si el correo se envió.
   *
   * @return {@code true} si el correo se envió (procede marcar el contrato).
   */
  @Override
  public boolean sendInicioOutsourcingNotification(InicioOutsourcingDTO dto) {
    List<String> contactosCliente = splitEmails(dto.getCorreosCliente());
    List<String> gestores = splitEmails(dto.getCorreosGestores());             // gestores del cliente
    List<String> seleccion = resolveMaestroEmails(Constante.MAESTRO_CORREO_SELECCION); // Selección (35)

    // Destinatario TO: contactos del RQ; si no hay, fallback a gestores del cliente;
    // si tampoco hay gestores, a Selección. Solo se omite si no hay absolutamente nadie.
    List<String> destinatarios = !contactosCliente.isEmpty() ? contactosCliente
        : (!gestores.isEmpty() ? gestores : seleccion);
    if (destinatarios.isEmpty()) {
      logger.warn("Notif. inicio contrato {}: sin destinatarios (ni contactos RQ, ni gestores, ni Selección); "
          + "se omite y reintentará.", dto.getIdContrato());
      return false;
    }

    // TO = primer destinatario; el resto va a CC junto con gestores + Selección.
    String to = destinatarios.get(0);
    List<String> cc = new ArrayList<>();
    if (destinatarios.size() > 1) {
      cc.addAll(destinatarios.subList(1, destinatarios.size()));
    }
    cc.addAll(gestores);
    cc.addAll(seleccion);
    List<String> cleanCc = deduplicateEmailList(cc);
    // evita que el TO se repita en CC cuando el fallback lo tomó de gestores/Selección
    cleanCc.removeIf(mail -> mail.equalsIgnoreCase(to));

    boolean esDiaInicio = "D0".equalsIgnoreCase(dto.getTipoHito());
    String nombreCompleto = (SafeValues.safeString(dto.getNombres()) + " "
        + SafeValues.safeString(dto.getApellidos())).trim();

    Map<String, Object> variables = new HashMap<>();
    variables.put("esDiaInicio", esDiaInicio);
    variables.put("nombreCompleto", nombreCompleto);
    variables.put("dni", SafeValues.safeString(dto.getDni()));
    variables.put("celular", SafeValues.safeString(dto.getCelular()));
    variables.put("correo", SafeValues.safeString(dto.getEmail()));
    variables.put("cargo", SafeValues.safeString(dto.getCargo()));
    variables.put("modalidad", SafeValues.safeString(dto.getModalidad()));
    variables.put("cliente", SafeValues.safeString(dto.getCliente()));
    variables.put("fchInicio", SafeValues.safeString(dto.getFchInicio()));

    String subject = (esDiaInicio ? "Inicio de labores hoy: " : "Próximo inicio de labores: ")
        + SafeValues.safeString(dto.getCliente()) + " - " + nombreCompleto;

    logger.info("Enviando notif. inicio ({}) contrato {} -> TO: {} | CC: {}",
        dto.getTipoHito(), dto.getIdContrato(), to, cleanCc.size());
    return mailUtils.sendTemplateEmailSync(EventoNotificacion.PRONTO_INICIO, to, cleanCc, subject,
        "inicio-outsourcing", variables);
  }

  @Async("notificationExecutor")
  @Override
  public void sendEquipmentRequestNotification(SolicitudEquipoReport report, Integer idTalento) {
    sendEquipmentRequestNotificationSync(report, idTalento);
  }

  @Override
  public boolean sendEquipmentRequestNotificationSync(SolicitudEquipoReport report, Integer idTalento) {
    try (var decision = correoSwitch.decision(EventoNotificacion.SOLICITUD_EQUIPO)) {
      if (!decision.habilitado()) {
        return false;
      }
      String employee = report.getNombreEmpleado() + " " + report.getApellidosEmpleado();

      List<FileDTO> attachments = this.reportPDFBuilder
          .fEquipoReport(report)
          .withFormulario()
          .build();

      var contacto = contactoTalento(idTalento, report.getNombreEmpleado(), report.getApellidosEmpleado(),
          report.getCelularTalento());

      // TO: maestro 51 (soporte). CC: generador + selección(35) + gestores del cliente.
      // El cuerpo lleva la tabla de contacto del talento.
      return sendFormularioEmail(EventoNotificacion.SOLICITUD_EQUIPO, attachments,
          Constante.MAESTRO_CORREO_SOPORTE, report.getCorreoGestor(), report.getGestoresCliente(),
          buildFormularioSubject("equipo", report.getCliente(), employee),
          FormularioCorreoUtils.cuerpoSolicitudHtml("equipo", List.of(contacto)), true);
    }
  }

  /**
   * Contacto del talento para la tabla de los correos de solicitud. Si no se
   * puede consultar, se arma con lo que trae el reporte.
   */
  private ContactoTalentoDTO contactoTalento(Integer idTalento, String nombres, String apellidos, String celular) {
    try {
      ContactoTalentoDTO contacto = employeeRepository.getContactoTalento(idTalento);
      if (contacto != null) {
        return contacto;
      }
    } catch (Exception e) {
      logger.warn("No se pudo obtener el contacto del talento {}: {}", idTalento, e.getMessage());
    }
    return new ContactoTalentoDTO(nombres, apellidos, "", celular != null ? celular : "");
  }
}
