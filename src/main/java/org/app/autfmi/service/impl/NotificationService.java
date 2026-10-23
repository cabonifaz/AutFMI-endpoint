package org.app.autfmi.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.app.autfmi.model.dto.ContactoTalentoDTO;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorRqDTO;
import org.app.autfmi.model.dto.ParametrosDTO;
import org.app.autfmi.model.dto.PostulantDTO;
import org.app.autfmi.model.dto.RequirementTalentsResult;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.repository.HistoryRepository;
import org.app.autfmi.service.IParametrosService;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.EventoNotificacion;
import org.app.autfmi.util.FormularioCorreoUtils;
import org.app.autfmi.util.MailUtils;
import org.app.autfmi.util.PDFUtils;
import org.app.autfmi.util.builders.ReportPDFBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class NotificationService {
  private final Logger logger = LoggerFactory.getLogger(NotificationService.class);

  // Los tres correos de la confirmación de talentos son una sola operación.
  private static final EventoNotificacion EVENTO = EventoNotificacion.RQ_TALENTOS_CONFIRMADOS;

  private final MailUtils mailUtils;
  private final PDFUtils pdfUtils;
  private final ReportPDFBuilder reportPDFBuilder;
  private final HistoryRepository historyRepository;
  private final IParametrosService parametrosService;
  private final NotificacionCorreoSwitch correoSwitch;

  @Async("notificationExecutor")
  public void sendRequirementNotifications(
      GestorRqDTO gestorRq,
      List<String> ccList,
      List<PostulantDTO> postulantes,
      List<RequirementTalentsResult.ReporteIngreso> entryReportsIds,
      List<RequirementTalentsResult.ReporteSolicitudEquipo> solicitudesEquipo,
      BaseRequest baseRequest
    ) {
    sendRequirementNotificationsSync(gestorRq, ccList, postulantes, entryReportsIds, solicitudesEquipo, baseRequest);
  }

  /**
   * Variante SÍNCRONA de {@link #sendRequirementNotifications}: genera los PDFs y
   * envía los tres correos (ingreso, creación de usuario, equipo) en el hilo que
   * llama. Se usa al confirmar el gestor, donde hay que saber si todo salió para
   * marcar a los talentos (o devolverlos a pendientes y reintentar).
   *
   * @return {@code true} solo si se enviaron todos los correos que correspondían.
   */
  public boolean sendRequirementNotificationsSync(
      GestorRqDTO gestorRq,
      List<String> ccList,
      List<PostulantDTO> postulantes,
      List<RequirementTalentsResult.ReporteIngreso> entryReportsIds,
      List<RequirementTalentsResult.ReporteSolicitudEquipo> solicitudesEquipo,
      BaseRequest baseRequest
    ) {

    boolean todoEnviado = true;

    // Una sola consulta para los tres correos; sin permiso no se generan los PDFs.
    try (var decision = correoSwitch.decision(EVENTO)) {

      if (!decision.habilitado()) {
        this.logger.info("Notificaciones de asignación de talentos omitidas ({}).", EVENTO.clave());
        return false;
      }

      this.logger.info("Iniciando envío de notificaciones");

      var gestorRqEmail = gestorRq.getCorreo();

      String cliente = gestorRq.getCliente();
      String codigoRQ = gestorRq.getCodigoRQ();

      if (gestorRqEmail == null) {
        this.logger.error("No se encontro el correo del gestor del requerimiento");
        return false;
      }

      if (ccList == null) {
        this.logger.warn("No se encontro la lista de correo de los cc");
        ccList = Collections.emptyList();
      }

      // CC base de los correos de AsignarTalento: ccList del RQ (gestores del cliente
      // + selección, ya SIN el gestor creador) + usuario generador (gestor del RQ) +
      // selección(35) por robustez. La limpieza/dedup final la hace enviarCorreoConPDF.
      List<String> baseCc = new ArrayList<>(ccList);
      if (!gestorRqEmail.trim().isEmpty()) {
        baseCc.add(gestorRqEmail.trim());
      }
      baseCc.addAll(resolveMaestroEmails(Constante.MAESTRO_CORREO_SELECCION));

      // Generar los PDFs de ingreso (Formulario de Ingreso) y creación de usuario
      // por cada talento. El de ingreso se adjunta al correo "Ingreso de nuevo
      // talento"; el de creación de usuario va en su propio correo al maestro 51.
      List<FileDTO> ingresoFiles = new ArrayList<>();
      List<FileDTO> usuarioFiles = new ArrayList<>();
      if (entryReportsIds != null) {
        this.logger.info("Generando formularios de ingreso y creación de usuario");
        entryReportsIds.forEach((report) -> {
          var idTalento = report.getIdTalento();
          if (idTalento == null) {
            this.logger.error("No se encontro el talento para el reporte de ingreso");
            return;
          }
          var entryReport = (EntryReport) this.historyRepository.getHistoryReport(
              baseRequest, idTalento, report.getIdTipoHistorial(), report.getIdHistorial(), false);
          ingresoFiles.addAll(this.reportPDFBuilder.forIngreso(entryReport).withFormulario().build());
          usuarioFiles.addAll(this.reportPDFBuilder.forIngreso(entryReport).withCreateUser().build());
        });
      }

      /* 1. Correo "Ingreso de nuevo talento": tabla de talentos + Formulario de
       *    Ingreso ADJUNTO. TO = gestorRq; CC = base + Talento(52). Se envía si hay
       *    postulantes O si hay formularios de ingreso que adjuntar. */
      boolean hayPostulantes = postulantes != null && !postulantes.isEmpty();
      if (hayPostulantes || !ingresoFiles.isEmpty()) {
        List<String> ccIngreso = new ArrayList<>(baseCc);
        ccIngreso.addAll(resolveMaestroEmails(Constante.MAESTRO_CORREO_TALENTO));
        todoEnviado &= this.mailUtils.sendRequirementPostulantMailSync(
            EVENTO,
            gestorRq,
            "Ingreso de nuevo talento",
            postulantes != null ? postulantes : Collections.emptyList(),
            ccIngreso,
            ingresoFiles);
        this.logger.info("Correo 'Ingreso de nuevo talento' procesado (adjuntos de ingreso: {})", ingresoFiles.size());
      }

      /* 2. Solicitud de Creación de Usuario -> maestro 51 (bundle de todos los talentos).
       *    El cuerpo lleva la tabla de contacto de los talentos ingresados. */
      if (!usuarioFiles.isEmpty()) {
        todoEnviado &= sendBundleToMaestro(usuarioFiles, Constante.MAESTRO_CORREO_SOPORTE, baseCc, gestorRqEmail,
            "Solicitud de creación de usuario: " + cliente + " - " + codigoRQ,
            FormularioCorreoUtils.cuerpoSolicitudHtml("creación de usuario", contactos(postulantes, null)));
      }

      /* 3. Solicitudes de equipo -> maestro 51 (bundle de todos los talentos). */
      if (solicitudesEquipo != null) {
        List<FileDTO> equipoFiles = new ArrayList<>();
        this.logger.info("Generando formularios de solicitudes de equipo");
        solicitudesEquipo.forEach((solicitud) -> {
          var reporte = this.historyRepository.getSolicitudEquipoReport(
              baseRequest, solicitud.getIdTalento(), solicitud.getIdSolicitudEquipo(), false);
          equipoFiles.addAll(this.reportPDFBuilder.fEquipoReport(reporte).withFormulario().build());
        });
        if (!equipoFiles.isEmpty()) {
          // Tabla de contacto solo de los talentos que tienen solicitud de equipo
          Set<Integer> conEquipo = new HashSet<>();
          solicitudesEquipo.forEach(sol -> conEquipo.add(sol.getIdTalento()));
          todoEnviado &= sendBundleToMaestro(equipoFiles, Constante.MAESTRO_CORREO_SOPORTE, baseCc, gestorRqEmail,
              "Solicitud de equipo: " + cliente + " - " + codigoRQ,
              FormularioCorreoUtils.cuerpoSolicitudHtml("equipo", contactos(postulantes, conEquipo)));
        }
      }

    } catch (Exception e) {
      this.logger.error("Error al enviar notificaciones: {}", e);
      return false;
    }

    return todoEnviado;
  }

  private List<ContactoTalentoDTO> contactos(List<PostulantDTO> postulantes, Set<Integer> soloTalentos) {
    List<ContactoTalentoDTO> contactos = new ArrayList<>();
    if (postulantes == null) {
      return contactos;
    }
    for (PostulantDTO p : postulantes) {
      if (soloTalentos == null || soloTalentos.contains(p.getIdTalento())) {
        contactos.add(ContactoTalentoDTO.from(p));
      }
    }
    return contactos;
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


  private boolean sendBundleToMaestro(List<FileDTO> files, String maestro, List<String> baseCc,
      String gestorRqEmail, String subject, String message) {
    List<String> toEmails = resolveMaestroEmails(maestro);
    String to = !toEmails.isEmpty()
        ? toEmails.get(0)
        : (gestorRqEmail != null ? gestorRqEmail.trim() : "");
    if (to == null || to.isEmpty()) {
      logger.error("Correo '{}' no enviado: sin destinatario (maestro {} ni gestorRq).", subject, maestro);
      return false;
    }
    List<String> cc = new ArrayList<>(baseCc);
    if (toEmails.size() > 1) {
      cc.addAll(toEmails.subList(1, toEmails.size()));
    }
    try {
      logger.info("Enviando '{}' -> TO: {} | CC: {}", subject, to, cc);
      return pdfUtils.enviarCorreoConPDF(EVENTO, files, to, cc, subject, message, true);
    } catch (Exception e) {
      logger.error("Error enviando '{}': ", subject, e);
      return false;
    }
  }

}
