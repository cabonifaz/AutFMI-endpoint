package org.app.autfmi.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.app.autfmi.model.dto.InicioOutsourcingDTO;
import org.app.autfmi.model.dto.ContactoTalentoDTO;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.ParametrosDTO;
import org.app.autfmi.repository.EmployeeRepository;
import org.app.autfmi.repository.ApprovalRepository;
import org.app.autfmi.model.dto.aprobacion.ApprovalDetailDTO;
import org.app.autfmi.util.ApprovalAreaPolicy;
import org.app.autfmi.util.ApprovalFormPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
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
import org.springframework.beans.factory.annotation.Value;
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

  @Autowired
  private SpringTemplateEngine emailTemplates;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ApprovalRepository approvalRepository;

  @Value("${approval.frontend.url:http://localhost:3000}")
  private String approvalFrontendUrl;

  private static final Logger logger = LoggerFactory.getLogger(MailService.class);

  public List<String> approvalRecipients(String area) {
    return resolveMaestroEmails("FMI".equals(area)?Constante.MAESTRO_CORREO_TALENTO:Constante.MAESTRO_CORREO_SOPORTE);
  }
  public List<String> approvalSelectionRecipients(){return resolveMaestroEmails(Constante.MAESTRO_CORREO_SELECCION);}

  public boolean sendApprovalArea(EventoNotificacion event, ApprovalDetailDTO detail,
      String area,String status,String reason,Object report,List<String> to,List<String> cc) throws Exception {
    if(to==null||to.isEmpty())return false;
    boolean rejected="RECHAZADA".equals(status);
    boolean returnEquipment = "DEVOLUCION".equals(status);
    List<FileDTO> files=new ArrayList<>();
    if(!rejected&&"FMI".equals(area)) {
      if(report instanceof EntryReport entry)files=reportPDFBuilder.forIngreso(entry).withFormulario().build();
      else if(report instanceof MovementReport movement)files=reportPDFBuilder.forMovimiento(movement).withFormulario().build();
      else if(report instanceof CeseReport cese)files=reportPDFBuilder.forCese(cese).withFormularioCese().build();
    }
    if(!rejected&&"EQUIPO".equals(area)&&report instanceof SolicitudEquipoReport equipo)files=reportPDFBuilder.fEquipoReport(equipo).withFormulario().build();
    if (!rejected && "CREDENCIALES".equals(area)) {
      files = credentialForm(detail, reason);
    }
    if (returnEquipment) {
      files = reportPDFBuilder.fEquipoReport(equipmentReturnReport(detail)).withDevolucion(reason).build();
    }
    String action=returnEquipment ? "Solicitud de devolución de equipo" : rejected ? "Desaprobada" : "Aprobada";
    String subject=action+" · "+area+" · Solicitud N° "+detail.getIdSolicitud()+" · "+SafeValues.safeString(detail.getNombreTalento());
    String body = renderApprovalNotice(detail, area, rejected, reason, report, files, returnEquipment);
    String target=to.get(0);List<String> copies=rejected?List.of():new ArrayList<>(cc);
    if(!rejected&&to.size()>1)copies.addAll(to.subList(1,to.size()));
    copies=rejected?new ArrayList<>():deduplicateEmailList(copies);copies.removeIf(v->v.equalsIgnoreCase(target));
    return pdfUtils.enviarCorreoConPDF(event,files,target,copies,subject,body,true);
  }

  private SolicitudEquipoReport equipmentReturnReport(ApprovalDetailDTO detail) throws Exception {
    var form = objectMapper.readTree(detail.getDatosFormulario());
    var equipment = ApprovalAreaPolicy.source(detail.getTipoSolicitud(), form, "EQUIPO");
    var report = new SolicitudEquipoReport();
    report.setNombreEmpleado(detail.getNombreTalento());
    report.setApellidosEmpleado("");
    report.setCliente(detail.getCliente());
    report.setArea(form.path("area").asText(""));
    report.setPuesto(form.path("cargo").asText(form.path("puesto").asText("")));
    report.setFechaSolicitud(java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")).toString());
    report.setFechaEntrega(form.path("fchDevolucionEquipo").asText(""));
    report.setIdTipoEquipo(equipment.path("idTipoEquipo").asInt());
    report.setProcesador(equipment.path("procesador").asText(""));
    report.setRam(equipment.path("ram").asText(""));
    report.setHd(equipment.path("hd").asText(""));
    report.setMarca(equipment.path("marca").asText(""));
    report.setAccesorios(equipment.path("accesorios").asText(""));
    report.setNombreApellidoGestor(detail.getUsuarioSolicitante());
    report.setCelularTalento(form.path("celularPersonal").asText(form.path("seleccion").path("celular").asText("")));
    return report;
  }

  private List<FileDTO> credentialForm(ApprovalDetailDTO detail, String reason) throws Exception {
    var form = detail.getDatosFormulario() == null ? objectMapper.createObjectNode()
        : objectMapper.readTree(detail.getDatosFormulario());
    var account = approvalRepository.getCorporateCredentials(detail.getIdTalento());
    var fields = ApprovalAreaPolicy.source(detail.getTipoSolicitud(), form, "CREDENCIALES");
    String date = fields.path("fecha").asText(form.path("fchInicioContrato").asText(detail.getFechaSolicitud()));
    String area = form.path("area").asText("");
    if ("CESE".equals(detail.getTipoSolicitud())) {
      var report = new CeseReport();
      report.setNombres(detail.getNombreTalento());
      report.setApellidos("");
      report.setUnidad(area);
      report.setFirmante(detail.getUsuarioSolicitante());
      report.setFechaHistorial(date);
      report.setMotivo(fields.path("detalle").asText(reason));
      report.setUsernameEmpleado(account.username());
      report.setEmailEmpleado(account.email());
      return reportPDFBuilder.forCese(report).withDeactivateRequest().build();
    }
    var report = new EntryReport();
    report.setNombres(detail.getNombreTalento());
    report.setApellidos("");
    report.setUnidad(area);
    report.setFirmante(detail.getUsuarioSolicitante());
    report.setFechaHistorial(date);
    report.setUsernameEmpleado(account.username());
    report.setEmailEmpleado(account.email());
    if ("CAMBIO".equals(form.path("accion").asText())) {
      return reportPDFBuilder.forIngreso(report).withModifyUser(fields.path("usuario").asText("")).build();
    }
    return reportPDFBuilder.forIngreso(report).withCreateUser().build();
  }

  String renderApprovalNotice(ApprovalDetailDTO detail, String area, boolean rejected,
      String reason, Object report, List<FileDTO> files) throws Exception {
    return renderApprovalNotice(detail, area, rejected, reason, report, files, false);
  }

  private String renderApprovalNotice(ApprovalDetailDTO detail, String area, boolean rejected,
      String reason, Object report, List<FileDTO> files, boolean returnEquipment) throws Exception {
    var context = new Context(java.util.Locale.forLanguageTag("es-PE"));
    context.setVariable("idSolicitud", detail.getIdSolicitud());
    context.setVariable("enlace", ApprovalNotifier.approvalLink(approvalFrontendUrl, detail.getIdSolicitud()));
    context.setVariable("area", switch (area) {
      case "EQUIPO" -> "Equipo";
      case "CREDENCIALES" -> "Credenciales";
      default -> area;
    });
    boolean fmi = "FMI".equals(area);
    var form = (!rejected || fmi) && detail.getDatosFormulario() != null
        ? objectMapper.readTree(detail.getDatosFormulario()) : objectMapper.createObjectNode();
    String talent = fmi
        ? (form.path("nombres").asText("").trim() + " " + form.path("apellidoPaterno").asText("").trim()).trim()
        : detail.getNombreTalento();
    context.setVariable("talento", talent);
    context.setVariable("cliente", detail.getCliente());
    context.setVariable("rechazada", rejected);
    context.setVariable("devolucion", returnEquipment);
    context.setVariable("motivo", reason);
    var fields = new LinkedHashMap<String, String>();
    var forms = new ArrayList<String>();
    var requirements = new LinkedHashMap<String, String>();
    if (!rejected) {
      String type = detail.getTipoSolicitud();

      var labels = ApprovalAreaPolicy.fields(type, area).stream().collect(java.util.stream.Collectors.toMap(
          org.app.autfmi.model.dto.aprobacion.ApprovalEditableFieldDTO::campo,
          org.app.autfmi.model.dto.aprobacion.ApprovalEditableFieldDTO::etiqueta));
      var values = ApprovalAreaPolicy.data(objectMapper, type, form, area);
      values.forEach((key, value) -> {
        if (value == null || value.toString().isBlank() || key.equals("idTipoEquipo")) return;
        if (fmi && !labels.containsKey(key) && !key.equals("nombres") && !key.equals("apellidoPaterno")) return;
        String label = switch (key) {
          case "accion" -> "Acción";
          case "tipoIngreso" -> "Tipo de ingreso";
          case "fechaIngresoAnterior" -> "Fecha del último ingreso";
          case "clienteAnterior" -> "Cliente anterior";
          case "cargoAnterior" -> "Cargo anterior";
          case "bonoEquipo" -> "Bono de equipo";
          default -> labels.getOrDefault(key, ApprovalFormPolicy.etiqueta(key));
        };
        String display = value instanceof Boolean flag ? (flag ? "Sí" : "No") : value.toString();
        if (key.equals("declararSunat") && value instanceof Number number) {
          display = number.intValue() == 1 ? "Sí" : "No";
        }
        fields.putIfAbsent(label, display);
      });
      if ("CREDENCIALES".equals(area)) {
        var account = approvalRepository.getCorporateCredentials(detail.getIdTalento());
        if (!account.username().isBlank()) fields.put("Usuario corporativo", account.username());
        if (!account.email().isBlank()) fields.put("Correo corporativo", account.email());
      }
      if ("FMI".equals(area) && !"INGRESO".equals(type)) {
        context.setVariable("instruccion", "Favor atender la solicitud de " + operationName(type) + ".");
      } else if (!"FMI".equals(area)) {
        String task = returnEquipment ? "Solicitud de devolución de equipo" : areaFormName(type, area, form.path("accion").asText());
        String email = form.path("correoPersonal").asText(form.path("seleccion").path("email").asText(""));
        String phone = form.path("celularPersonal").asText(form.path("seleccion").path("celular").asText(""));
        var contact = new ContactoTalentoDTO(
            form.path("nombres").asText(detail.getNombreTalento()),
            String.join(" ", form.path("apellidoPaterno").asText(""), form.path("apellidoMaterno").asText("")).trim(),
            email, phone);
        context.setVariable("instruccion", "Favor atender la " + task.toLowerCase(java.util.Locale.ROOT) + ".");
        context.setVariable("tablaContacto", FormularioCorreoUtils.tablaContactoHtml(List.of(contact)));
      }
      if ("INGRESO".equals(type) && "FMI".equals(area)) {
        var summary = new LinkedHashMap<String, String>();
        summary.put("Talento", talent);
        summary.put("Celular", fields.remove("Número de celular"));
        summary.put("Correo personal", fields.remove("Correo personal"));
        summary.put("Inicio de labores", fields.remove("Inicio de contrato"));
        summary.put("Cargo", fields.remove("Cargo"));
        summary.put("Remuneración", fields.remove("Remuneración"));
        if (report instanceof EntryReport entry) summary.put("Modalidad", entry.getModalidad());
        int ownEquipment = form.path("tieneEquipo").asInt(-1);
        if (ownEquipment >= 0) {
          fields.remove("Equipo propio");
          summary.put("Equipo propio", ownEquipment == 1 ? "Sí" : "No");
        }
        summary.values().removeIf(value -> value == null || value.isBlank());
        context.setVariable("resumenIngreso", summary);
        int requirement = form.path("idRequerimiento").asInt();
        if (requirement > 0) {
          try {
            var rq = approvalRepository.getEntryRequirements(requirement, detail.getIdCliente());
            context.setVariable("codigoRq", rq.get("CODIGO_RQ"));
            if (!rq.isEmpty()) {
              requirements.put("EMO", requirementMark(rq.get("EMO")));
              requirements.put("SCTR", requirementMark(rq.get("SCTR")));
              requirements.put("Asignación de laptop", requirementMark(rq.get("ASIGNACION_LAPTOP")));
            }
          } catch (org.springframework.dao.DataAccessException e) {
            logger.warn("No se pudieron consultar los requisitos del RQ {} para el correo.", requirement, e);
          }
        }
      }
      if (!files.isEmpty() && fmi) {
        for (FileDTO file : files) {
          String filename = SafeValues.safeString(file.getNombreArchivo());
          int code = filename.lastIndexOf(" - FT-");
          String title = code >= 0 ? filename.substring(code + 3)
              : filename.startsWith("FT-") ? filename : areaFormName(type, area, "");
          forms.add((talent.isBlank() ? "" : talent + " - ") + title + " (PDF adjunto)");
        }
      }
      else if (!files.isEmpty()) files.forEach(file -> forms.add(file.getNombreArchivo() + " (PDF adjunto)"));
      else forms.add(areaFormName(type, area, form.path("accion").asText()));
    }
    context.setVariable("campos", fields);
    context.setVariable("formularios", forms);
    context.setVariable("requisitos", requirements);
    return emailTemplates.process("aprobacion-area", context);
  }

  private static String operationName(String type) {
    return switch (type) {
      case "INGRESO" -> "ingreso";
      case "MOVIMIENTO" -> "movimiento";
      case "CESE" -> "cese";
      default -> "aprobación";
    };
  }

  private static String areaFormName(String type, String area, String action) {
    return switch (area) {
      case "EQUIPO" -> "CESE".equals(type) ? "Solicitud de devolución de equipo" : "Solicitud de equipo";
      case "CREDENCIALES" -> "CESE".equals(type) ? "Solicitud de baja de credenciales"
          : "CAMBIO".equals(action) || "MOVIMIENTO".equals(type) ? "Solicitud de cambio de credenciales"
          : "Solicitud de creación de credenciales";
      default -> "Formulario de " + operationName(type);
    };
  }

  private static String requirementMark(Object value) {
    if (value == null) return "Sin información";
    return Boolean.TRUE.equals(value) || value instanceof Number number && number.intValue() == 1 ? "☑" : "☐";
  }

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

  private String formularioBodyConEnlace(String accion, String enlace) {
    if (enlace == null) return buildFormularioBody(accion);
    return FormularioCorreoUtils.conEnlaceAprobacion(
        "<div style='font-family:Arial,sans-serif'><p>Estimados,</p><p>Favor atender la solicitud de "
            + org.springframework.web.util.HtmlUtils.htmlEscape(accion) + ".</p><p>Saludos,</p></div>", enlace);
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
    return sendEntryReportNotificationSync(report, idTalento, null);
  }

  @Override
  public boolean sendEntryReportNotificationSync(EntryReport report, Integer idTalento, String enlace) {
    // Una sola consulta para los dos correos; sin permiso no se generan los PDFs.
    try (var decision = correoSwitch.decision(EventoNotificacion.INGRESO)) {
      return decision.habilitado() && enviarFormulariosIngreso(report, idTalento, enlace);
    }
  }

  private boolean enviarFormulariosIngreso(EntryReport report, Integer idTalento, String enlace) {
    String fullname = report.getNombres() + " " + report.getApellidos();
    String cliente = report.getCliente();
    String gestores = report.getGestoresCliente();

    // Correo 1: Formulario de Ingreso -> maestro 52 (talento)
    boolean ingreso = sendFormularioEmail(EventoNotificacion.INGRESO,
        this.reportPDFBuilder.forIngreso(report).withFormulario().build(),
        Constante.MAESTRO_CORREO_TALENTO, report.getCorreoGestor(), gestores,
        buildFormularioSubject("ingreso", cliente, fullname),
        formularioBodyConEnlace("ingreso", enlace), enlace != null);

    // Correo 2: Solicitud de Creación de Usuario -> maestro 51 (soporte), con la
    // tabla de contacto del talento en el cuerpo.
    var contacto = contactoTalento(idTalento, report.getNombres(), report.getApellidos(), null);
    boolean usuario = sendFormularioEmail(EventoNotificacion.INGRESO,
        this.reportPDFBuilder.forIngreso(report).withCreateUser().build(),
        Constante.MAESTRO_CORREO_SOPORTE, report.getCorreoGestor(), gestores,
        buildFormularioSubject("creación de usuario", cliente, fullname),
        FormularioCorreoUtils.conEnlaceAprobacion(FormularioCorreoUtils.cuerpoSolicitudHtml("creación de usuario", List.of(contacto)), enlace), true);

    return ingreso && usuario;
  }

  @Async("notificationExecutor")
  @Override
  public void sendCeseReportNotification(CeseReport report) {
    sendCeseReportNotificationSync(report);
  }

  @Override
  public boolean sendCeseReportNotificationSync(CeseReport report) {
    return sendCeseReportNotificationSync(report, null);
  }

  @Override
  public boolean sendCeseReportNotificationSync(CeseReport report, String enlace) {
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
          formularioBodyConEnlace("cese", enlace), enlace != null);
    }
  }

  @Override
  public void sendMovementReportNotification(MovementReport report) {
    sendMovementReportNotificationSync(report);
  }

  @Override
  public boolean sendMovementReportNotificationSync(MovementReport report) {
    return sendMovementReportNotificationSync(report, null);
  }

  @Override
  public boolean sendMovementReportNotificationSync(MovementReport report, String enlace) {
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
          formularioBodyConEnlace("movimiento", enlace), enlace != null);
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
    // Los contactos del cliente ligados al RQ (dto.getCorreosCliente()) ya no
    // reciben este correo: va solo a los gestores del cliente y a Selección.
    List<String> gestores = splitEmails(dto.getCorreosGestores());             // gestores del cliente
    List<String> seleccion = resolveMaestroEmails(Constante.MAESTRO_CORREO_SELECCION); // Selección (35)

    // Destinatario TO: gestores del cliente; si no hay, Selección. Solo se omite
    // si no hay absolutamente nadie.
    List<String> destinatarios = !gestores.isEmpty() ? gestores : seleccion;
    if (destinatarios.isEmpty()) {
      logger.warn("Notif. inicio contrato {}: sin destinatarios (ni gestores, ni Selección); "
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
