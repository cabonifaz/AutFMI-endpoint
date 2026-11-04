package org.app.autfmi.service.impl;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.*;
import org.app.autfmi.model.dto.aprobacion.*;
import org.app.autfmi.model.report.IReport;
import org.app.autfmi.repository.*;
import org.app.autfmi.util.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Cada decisión tiene su propia reserva de correo. Desaprobar jamás añade CC. */
@Service
@RequiredArgsConstructor
public class ApprovalAreaNotifier {

  private final ApprovalRepository requests;
  private final ApprovalAreaRepository areas;
  private final ApprovalNotifier contacts;
  private final org.app.autfmi.service.IMailService mail;
  private final NotificacionCorreoSwitch switches;
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(
    ApprovalAreaNotifier.class
  );

  @Async("notificationExecutor")
  public void notifyArea(
    ApprovalDetailDTO detail,
    String area,
    String status,
    String reason,
    Object report,
    UserDTO actor
  ) {
    send(detail, area, status, reason, report, actor);
  }

  String send(
    ApprovalDetailDTO detail,
    String area,
    String status,
    String reason,
    Object report,
    UserDTO actor
  ) {
    boolean rejected = "RECHAZADA".equals(status);
    String key = (rejected ? "RECHAZO_" : "AREA_") + area;
    if (
      !requests.reserveMail(
        detail.getIdSolicitud(),
        key,
        actor.getUsuario(),
        actor.getIdUsuario()
      )
    ) return null;
    String result = "FALLIDO";
    try {
      EventoNotificacion event = rejected
        ? EventoNotificacion.APROBACION_GESTORES
        : switch (detail.getTipoSolicitud()) {
            case "INGRESO", "CREDENCIALES" -> EventoNotificacion.INGRESO;
            case "MOVIMIENTO" -> EventoNotificacion.MOVIMIENTO;
            case "CESE" -> EventoNotificacion.CESE;
            default -> EventoNotificacion.SOLICITUD_EQUIPO;
          };
      if (!switches.permitido(event)) result = "OMITIDO";
      else {
        var applicant = areas.applicant(
          detail.getIdSolicitud(),
          actor.getIdEmpresa()
        );
        var applicantEmails = contacts.correosGestores(
          applicant == null ? List.of() : List.of(applicant),
          actor.getIdEmpresa()
        );
        List<String> to;
        List<String> cc = new ArrayList<>();
        if (rejected) to = applicantEmails;
        else {
          to = mail.approvalRecipients(area);
          cc.addAll(applicantEmails);
          cc.addAll(
            contacts.correosGestores(
              areas.managers(detail.getIdCliente(), actor.getIdEmpresa()),
              actor.getIdEmpresa()
            )
          );
          cc.addAll(mail.approvalSelectionRecipients());
        }
        result = mail.sendApprovalArea(
          event,
          detail,
          area,
          status,
          reason,
          report,
          to,
          cc
        )
          ? "ENVIADO"
          : "FALLIDO";
      }
    } catch (Exception e) {
      log.error(
        "No se pudo notificar decisión de área {} de solicitud {}",
        area,
        detail.getIdSolicitud(),
        e
      );
    }
    requests.mailResult(
      detail.getIdSolicitud(),
      key,
      result,
      (rejected ? "Solo solicitante: " : "Área ") + area,
      actor.getUsuario(),
      actor.getIdUsuario()
    );
    return result;
  }
}
