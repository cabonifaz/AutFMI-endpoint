package org.app.autfmi.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorRqDTO;
import org.app.autfmi.model.dto.PostulantDTO;
import org.app.autfmi.model.dto.RequirementTalentsResult;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.repository.HistoryRepository;
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

      String subject = "Ingreso nuevo talento | " + gestorRq.getCodigoRQ() + " | " + gestorRq.getCliente();

      if (gestorRqEmail == null) {
        this.logger.error("No se encontro el correo del gestor del requerimiento");
        return;
      }

      if (ccList == null) {
        this.logger.warn("No se encontro la lista de correo de los cc");
        ccList = Collections.emptyList();
      }

      /* 1. Notificar sobre talentos confirmados */
      if (postulantes != null && !postulantes.isEmpty()) {
        this.mailUtils.sendRequirementPostulantMail(
          gestorRq,
          "Ingreso de nuevo talento",
          postulantes,
          ccList
        );
        this.logger.info("Notificación de talentos confirmados enviada");
      }

      /* 2. Formularos de ingresos IDs */
      if (entryReportsIds != null) {
        this.logger.info("Generando formularos de ingresos");
        List<FileDTO> filesToSend = new ArrayList<>();
        entryReportsIds.stream().forEach((report) -> {

          var idTalento = report.getIdTalento();

          if (idTalento == null) {
            this.logger.error("No se encontro el talento para el reporte de ingreso");
            return;
          }

          var entryReport = (EntryReport) this.historyRepository.getHistoryReport(
              baseRequest,
              idTalento,
              report.getIdTipoHistorial(),
              report.getIdHistorial(),
              false);

          var files = this.reportPDFBuilder
              .forIngreso(entryReport)
              .withFormulario()
              .withCreateUser()
              .build();

          filesToSend.addAll(files);
        });

        this.logger.info("Formularios de ingresos generados: {}", filesToSend.size());

        if (!filesToSend.isEmpty()) {

          this.logger.info("Enviando formularios de ingresos");
          pdfUtils.enviarCorreoConPDF(
              filesToSend,
              gestorRqEmail,
              ccList != null ? ccList : Collections.emptyList(),
              subject,
              "Formulario de nuevo ingreso de empleado.");
          filesToSend.clear();
          this.logger.info("Formularios de ingresos enviados");
        }
      }

      /** Notificar sobre solicitudes de equipo */
      if (solicitudesEquipo != null) {
        List<FileDTO> filesToSend = new ArrayList<>();
        // Obtener las solicitudes de equipo
        this.logger.info("Generando formularos de solicitudes de equipo");
        solicitudesEquipo.stream().forEach((solicitud) -> {

          var reporte = this.historyRepository.getSolicitudEquipoReport(
              baseRequest,
              solicitud.getIdTalento(),
              solicitud.getIdSolicitudEquipo(),
              false);

          var files = this.reportPDFBuilder
              .fEquipoReport(reporte)
              .withFormulario()
              .build();
          filesToSend.addAll(files);
        });

        this.logger.info("Formularos de solicitudes de equipo generados: {}", filesToSend.size());

        if (!filesToSend.isEmpty()) {
          this.logger.info("Enviando formularos de solicitudes de equipo");
          pdfUtils.enviarCorreoConPDF(
              filesToSend,
              gestorRqEmail,
              ccList != null ? ccList : Collections.emptyList(),
              "Solicitud de equipo " + subject,
              "Formulario de solicitud de equipo.");
          filesToSend.clear();
          this.logger.info("Formularos de solicitudes de equipo enviados");
        }
      }

    } catch (Exception e) {
      this.logger.error("Error al enviar notificaciones: {}", e);
    }

  }

}
