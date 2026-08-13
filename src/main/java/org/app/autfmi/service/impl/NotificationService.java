package org.app.autfmi.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

  private final MailUtils mailUtils;
  private final PDFUtils pdfUtils;
  private final ReportPDFBuilder reportPDFBuilder;
  private final HistoryRepository historyRepository;
  private final IParametrosService parametrosService;

  @Async("notificationExecutor")
  public void sendRequirementNotifications(
      GestorRqDTO gestorRq,
      List<String> ccList,
      List<PostulantDTO> postulantes,
      List<RequirementTalentsResult.ReporteIngreso> entryReportsIds,
      List<RequirementTalentsResult.ReporteSolicitudEquipo> solicitudesEquipo,
      BaseRequest baseRequest
    ) {

    try {

      this.logger.info("Iniciando envío asíncrono de notificaciones");

      var gestorRqEmail = gestorRq.getCorreo();

      String subjectBase = gestorRq.getCodigoRQ() + " | " + gestorRq.getCliente();

      if (gestorRqEmail == null) {
        this.logger.error("No se encontro el correo del gestor del requerimiento");
        return;
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
        this.mailUtils.sendRequirementPostulantMail(
            gestorRq,
            "Ingreso de nuevo talento",
            postulantes != null ? postulantes : Collections.emptyList(),
            ccIngreso,
            ingresoFiles);
        this.logger.info("Correo 'Ingreso de nuevo talento' enviado (adjuntos de ingreso: {})", ingresoFiles.size());
      }

      /* 2. Solicitud de Creación de Usuario -> maestro 51 (bundle de todos los talentos). */
      if (!usuarioFiles.isEmpty()) {
        sendBundleToMaestro(usuarioFiles, Constante.MAESTRO_CORREO_SOPORTE, baseCc, gestorRqEmail,
            "Solicitud de Creación de Usuario | " + subjectBase,
            "Solicitud de creación de usuario(s).");
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
          sendBundleToMaestro(equipoFiles, Constante.MAESTRO_CORREO_SOPORTE, baseCc, gestorRqEmail,
              "Solicitud de Equipo | " + subjectBase,
              "Formulario de solicitud de equipo.");
        }
      }

    } catch (Exception e) {
      this.logger.error("Error al enviar notificaciones: {}", e);
    }

  }

  /**
   * Resuelve TODOS los correos (string1) del PARAMETROS del maestro indicado. Un
   * maestro puede tener varias filas (varios correos); se devuelven todos.
   */
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

  /**
   * Envía un bundle de PDFs a un maestro: TO = primer correo del maestro (si tiene
   * varios, el resto van a CC); si el maestro no resuelve, cae al generador. CC =
   * baseCc + correos extra del maestro.
   */
  private void sendBundleToMaestro(List<FileDTO> files, String maestro, List<String> baseCc,
      String gestorRqEmail, String subject, String message) {
    List<String> toEmails = resolveMaestroEmails(maestro);
    String to = !toEmails.isEmpty()
        ? toEmails.get(0)
        : (gestorRqEmail != null ? gestorRqEmail.trim() : "");
    if (to == null || to.isEmpty()) {
      logger.error("Correo '{}' no enviado: sin destinatario (maestro {} ni gestorRq).", subject, maestro);
      return;
    }
    List<String> cc = new ArrayList<>(baseCc);
    if (toEmails.size() > 1) {
      cc.addAll(toEmails.subList(1, toEmails.size()));
    }
    try {
      logger.info("Enviando '{}' -> TO: {} | CC: {}", subject, to, cc);
      pdfUtils.enviarCorreoConPDF(files, to, cc, subject, message);
    } catch (Exception e) {
      logger.error("Error enviando '{}': ", subject, e);
    }
  }

}
