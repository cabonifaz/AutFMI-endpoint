package org.app.autfmi.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.app.autfmi.model.dto.UserContactInfoDTO;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalGestorDTO;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.IReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.repository.ApprovalRepository;
import org.app.autfmi.repository.UserRepository;
import org.app.autfmi.service.IMailService;
import org.app.autfmi.util.ApprovalFormPolicy;
import org.app.autfmi.util.EmailUtils;
import org.app.autfmi.util.EventoNotificacion;
import org.app.autfmi.util.MailUtils;
import org.app.autfmi.util.SafeValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Correos del flujo de aprobación (FTBO-1124). Cada envío se reserva antes en
 * FMI_SOLICITUD_APROBACION_CORREO (no hay dos envíos del mismo correo) y su
 * resultado queda en la línea de tiempo. Respeta los interruptores del
 * maestro 56 (FTBO-1117): con el del evento apagado, el envío queda OMITIDO.
 */
@Service
public class ApprovalNotifier {

  static final String CORREO_GESTORES = "GESTORES";
  static final String CORREO_AREAS = "AREAS";
  static final String ENVIADO = "ENVIADO";
  static final String OMITIDO = "OMITIDO";
  static final String FALLIDO = "FALLIDO";

  private static final Logger logger = LoggerFactory.getLogger(ApprovalNotifier.class);

  private final ApprovalRepository approvalRepository;
  private final UserRepository userRepository;
  private final MailUtils mailUtils;
  private final IMailService mailService;
  private final NotificacionCorreoSwitch correoSwitch;
  private final ObjectMapper objectMapper;
  private final String frontendUrl;

  public ApprovalNotifier(ApprovalRepository approvalRepository, UserRepository userRepository, MailUtils mailUtils,
      IMailService mailService, NotificacionCorreoSwitch correoSwitch, ObjectMapper objectMapper,
      @Value("${approval.frontend.url:http://localhost:3000}") String frontendUrl) {
    this.approvalRepository = approvalRepository;
    this.userRepository = userRepository;
    this.mailUtils = mailUtils;
    this.mailService = mailService;
    this.correoSwitch = correoSwitch;
    this.objectMapper = objectMapper;
    this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
  }

  /** Datos de una solicitud recién registrada para avisar a los gestores. */
  public record GestorNotice(Integer idSolicitud, String tipo, String cliente, String nombreTalento, String resumen,
      String datosFormulario, List<ApprovalGestorDTO> gestores, UserDTO solicitante) {
  }

  /** Enlace directo a la solicitud en la bandeja (HashRouter). */
  String enlace(Integer idSolicitud) {
    return frontendUrl + "/#/bandejaAprobaciones/" + idSolicitud;
  }

  @Async("notificationExecutor")
  public void notifyGestores(GestorNotice n) {
    notifyGestoresSync(n);
  }

  /** @return estado final del envío (ENVIADO | OMITIDO | FALLIDO), o null si otro ya lo tenía reservado. */
  String notifyGestoresSync(GestorNotice n) {
    UserDTO solicitante = n.solicitante();
    if (!approvalRepository.reserveMail(n.idSolicitud(), CORREO_GESTORES, solicitante.getUsuario(),
        solicitante.getIdUsuario())) {
      logger.info("Correo a gestores de la solicitud {} ya enviado o en curso: no se repite.", n.idSolicitud());
      return null;
    }

    String estado;
    String detalle;
    try {
      if (!correoSwitch.permitido(EventoNotificacion.APROBACION_GESTORES)) {
        estado = OMITIDO;
        detalle = "interruptor " + EventoNotificacion.APROBACION_GESTORES.clave() + " desactivado (maestro 56)";
      } else {
        List<String> correos = correosGestores(n.gestores(), solicitante.getIdEmpresa());
        if (correos.isEmpty()) {
          estado = FALLIDO;
          detalle = "el cliente no tiene gestores con correo registrado";
        } else {
          String to = correos.get(0);
          List<String> cc = correos.subList(1, correos.size());
          String subject = "Solicitud de aprobación N° " + n.idSolicitud() + " - " + etiquetaTipo(n.tipo()) + ": "
              + SafeValues.safeString(n.cliente()) + " - " + SafeValues.safeString(n.nombreTalento());
          boolean ok = mailUtils.sendTemplateEmailSync(EventoNotificacion.APROBACION_GESTORES, to, cc, subject,
              "aprobacion-gestores", variablesGestores(n));
          estado = ok ? ENVIADO : FALLIDO;
          detalle = (ok ? "enviado a " : "no se pudo enviar a ") + correos.size()
              + (correos.size() == 1 ? " gestor" : " gestores");
        }
      }
    } catch (Exception e) {
      logger.error("Error en el correo a gestores de la solicitud {}: ", n.idSolicitud(), e);
      estado = FALLIDO;
      detalle = "error al preparar el correo";
    }

    approvalRepository.mailResult(n.idSolicitud(), CORREO_GESTORES, estado, detalle, solicitante.getUsuario(),
        solicitante.getIdUsuario());
    logger.info("Correo a gestores de la solicitud {}: {} ({}).", n.idSolicitud(), estado, detalle);
    return estado;
  }

  @Async("notificationExecutor")
  public void notifyAreas(Integer idSolicitud, String tipo, IReport report, Integer idTalento, UserDTO aprobador) {
    notifyAreasSync(idSolicitud, tipo, report, idTalento, aprobador);
  }

  /**
   * Correos de siempre de la operación, con sus formularios PDF: Talento
   * (maestro 52), Soporte (maestro 51, creación de usuario en el ingreso) y en
   * copia los gestores del cliente, Selección (35) y quien aprobó.
   */
  String notifyAreasSync(Integer idSolicitud, String tipo, IReport report, Integer idTalento, UserDTO aprobador) {
    if (!approvalRepository.reserveMail(idSolicitud, CORREO_AREAS, aprobador.getUsuario(), aprobador.getIdUsuario())) {
      logger.info("Correos a áreas de la solicitud {} ya enviados o en curso: no se repiten.", idSolicitud);
      return null;
    }

    String estado;
    String detalle;
    try {
      EventoNotificacion evento = switch (tipo) {
        case "INGRESO" -> EventoNotificacion.INGRESO;
        case "MOVIMIENTO" -> EventoNotificacion.MOVIMIENTO;
        default -> EventoNotificacion.CESE;
      };
      if (report == null) {
        estado = OMITIDO;
        detalle = "la operación se registró sin reporte para los formularios";
      } else if (!correoSwitch.permitido(evento)) {
        estado = OMITIDO;
        detalle = "interruptor " + evento.clave() + " desactivado (maestro 56)";
      } else {
        boolean ok = switch (tipo) {
          case "INGRESO" -> mailService.sendEntryReportNotificationSync((EntryReport) report, idTalento);
          case "MOVIMIENTO" -> mailService.sendMovementReportNotificationSync((MovementReport) report);
          default -> mailService.sendCeseReportNotificationSync((CeseReport) report);
        };
        estado = ok ? ENVIADO : FALLIDO;
        detalle = "INGRESO".equals(tipo)
            ? "Talento (formulario de ingreso) y Soporte (creación de usuario), gestores en copia"
            : "Talento (formulario de " + tipo.toLowerCase() + "), gestores en copia";
      }
    } catch (Exception e) {
      logger.error("Error en los correos a áreas de la solicitud {}: ", idSolicitud, e);
      estado = FALLIDO;
      detalle = "error al preparar los correos";
    }

    approvalRepository.mailResult(idSolicitud, CORREO_AREAS, estado, detalle, aprobador.getUsuario(),
        aprobador.getIdUsuario());
    logger.info("Correos a áreas de la solicitud {}: {} ({}).", idSolicitud, estado, detalle);
    return estado;
  }

  private List<String> correosGestores(List<ApprovalGestorDTO> gestores, Integer idEmpresa) {
    Set<String> correos = new LinkedHashSet<>();
    if (gestores == null) {
      return new ArrayList<>();
    }
    for (ApprovalGestorDTO gestor : gestores) {
      try {
        // Mismo SP que usa el resto del backend para el contacto de un usuario.
        UserContactInfoDTO contacto = userRepository.getUserContactInfo(
            new BaseRequest(gestor.idUsuario(), gestor.usuario(), idEmpresa, gestor.idTipoRol(), ""));
        if (contacto != null && EmailUtils.isValidEmail(contacto.email())) {
          correos.add(contacto.email().trim());
        } else {
          logger.warn("El gestor {} no tiene correo válido registrado.", gestor.idUsuario());
        }
      } catch (Exception e) {
        logger.warn("No se pudo obtener el correo del gestor {}: {}", gestor.idUsuario(), e.getMessage());
      }
    }
    return new ArrayList<>(correos);
  }

  private Map<String, Object> variablesGestores(GestorNotice n) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("idSolicitud", n.idSolicitud());
    variables.put("tipo", etiquetaTipo(n.tipo()));
    variables.put("cliente", SafeValues.safeString(n.cliente()));
    variables.put("talento", SafeValues.safeString(n.nombreTalento()));
    variables.put("resumen", SafeValues.safeString(n.resumen()));
    variables.put("solicitante", SafeValues.safeString(n.solicitante().getUsuario()));
    variables.put("filas", ApprovalFormPolicy.resumenFormulario(objectMapper, n.datosFormulario()));
    variables.put("enlace", enlace(n.idSolicitud()));
    return variables;
  }

  static String etiquetaTipo(String tipo) {
    return switch (tipo == null ? "" : tipo) {
      case "INGRESO" -> "Ingreso";
      case "MOVIMIENTO" -> "Movimiento";
      case "CESE" -> "Cese";
      default -> SafeValues.safeString(tipo);
    };
  }
}
