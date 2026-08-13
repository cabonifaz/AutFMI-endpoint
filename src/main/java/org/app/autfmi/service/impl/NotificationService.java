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

      // CC de los formularios: ccList del RQ + usuario generador (gestor del RQ) +
      // maestro 35 (selección). La deduplicación/limpieza final la hace enviarCorreoConPDF.
      List<String> formCc = new ArrayList<>(ccList);
      if (!gestorRqEmail.trim().isEmpty()) {
        formCc.add(gestorRqEmail.trim());
      }
      String seleccion = resolveMaestroEmail(Constante.MAESTRO_CORREO_SELECCION);
      if (!seleccion.isEmpty()) {
        formCc.add(seleccion);
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

      /* 2. Formularios de ingreso: se SEPARAN por tipo/destino.
       *    Formulario de Ingreso -> maestro 52; Creación de Usuario -> maestro 51.
       *    Cada tipo se agrupa (bundle) en su propio correo con todos los talentos. */
      if (entryReportsIds != null) {
        this.logger.info("Generando formularios de ingreso y creación de usuario");
        List<FileDTO> ingresoFiles = new ArrayList<>();
        List<FileDTO> usuarioFiles = new ArrayList<>();

        entryReportsIds.forEach((report) -> {
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

          ingresoFiles.addAll(this.reportPDFBuilder.forIngreso(entryReport).withFormulario().build());
          usuarioFiles.addAll(this.reportPDFBuilder.forIngreso(entryReport).withCreateUser().build());
        });

        // Correo A: Formulario de Ingreso -> maestro 52
        if (!ingresoFiles.isEmpty()) {
          String to = resolveFormularioTo(Constante.MAESTRO_CORREO_TALENTO, gestorRqEmail);
          this.logger.info("Enviando Formularios de Ingreso -> TO: {} | CC: {}", to, formCc);
          pdfUtils.enviarCorreoConPDF(
              ingresoFiles,
              to,
              formCc,
              "Formulario de Ingreso | " + subjectBase,
              "Formulario de ingreso de empleado(s).");
        }

        // Correo B: Solicitud de Creación de Usuario -> maestro 51
        if (!usuarioFiles.isEmpty()) {
          String to = resolveFormularioTo(Constante.MAESTRO_CORREO_SOPORTE, gestorRqEmail);
          this.logger.info("Enviando Solicitudes de Creación de Usuario -> TO: {} | CC: {}", to, formCc);
          pdfUtils.enviarCorreoConPDF(
              usuarioFiles,
              to,
              formCc,
              "Solicitud de Creación de Usuario | " + subjectBase,
              "Solicitud de creación de usuario(s).");
        }
      }

      /* 3. Solicitudes de equipo -> maestro 51 (bundle de todos los talentos). */
      if (solicitudesEquipo != null) {
        List<FileDTO> equipoFiles = new ArrayList<>();
        this.logger.info("Generando formularios de solicitudes de equipo");
        solicitudesEquipo.forEach((solicitud) -> {
          var reporte = this.historyRepository.getSolicitudEquipoReport(
              baseRequest,
              solicitud.getIdTalento(),
              solicitud.getIdSolicitudEquipo(),
              false);
          equipoFiles.addAll(this.reportPDFBuilder.fEquipoReport(reporte).withFormulario().build());
        });

        if (!equipoFiles.isEmpty()) {
          String to = resolveFormularioTo(Constante.MAESTRO_CORREO_SOPORTE, gestorRqEmail);
          this.logger.info("Enviando Solicitudes de Equipo -> TO: {} | CC: {}", to, formCc);
          pdfUtils.enviarCorreoConPDF(
              equipoFiles,
              to,
              formCc,
              "Solicitud de Equipo | " + subjectBase,
              "Formulario de solicitud de equipo.");
        }
      }

    } catch (Exception e) {
      this.logger.error("Error al enviar notificaciones: {}", e);
    }

  }

  /**
   * Resuelve el correo destino de un formulario desde el string1 del PARAMETROS
   * del maestro indicado. Se asume una fila por maestro: se toma el primer
   * string1 no vacío. Devuelve "" si no se resuelve.
   */
  private String resolveMaestroEmail(String maestro) {
    try {
      var response = parametrosService.listParametros(maestro);
      if (response == null || response.getListParametros() == null) {
        return "";
      }
      return response.getListParametros().stream()
          .map(ParametrosDTO::getString1)
          .filter(s -> s != null && !s.trim().isEmpty())
          .map(String::trim)
          .findFirst()
          .orElse("");
    } catch (Exception e) {
      logger.warn("No se pudo resolver el correo del maestro {}: {}", maestro, e.getMessage());
      return "";
    }
  }

  /**
   * TO de un formulario: el correo del maestro; si no se resuelve, cae al correo
   * del generador (gestor del RQ) para no perder el envío.
   */
  private String resolveFormularioTo(String maestro, String fallback) {
    String email = resolveMaestroEmail(maestro);
    if (!email.isEmpty()) {
      return email;
    }
    logger.warn("Maestro {} sin correo configurado; se usa el correo del generador como destinatario", maestro);
    return fallback != null ? fallback.trim() : "";
  }

}
