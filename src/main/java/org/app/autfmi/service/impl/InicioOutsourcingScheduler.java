package org.app.autfmi.service.impl;

import java.util.List;

import org.app.autfmi.model.dto.InicioOutsourcingDTO;
import org.app.autfmi.repository.InicioNotificationRepository;
import org.app.autfmi.service.IMailService;
import org.app.autfmi.util.EventoNotificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InicioOutsourcingScheduler {

  private final Logger logger = LoggerFactory.getLogger(InicioOutsourcingScheduler.class);

  private final InicioNotificationRepository repository;
  private final IMailService mailService;
  private final NotificacionCorreoSwitch correoSwitch;

  // Interruptor del envío (propiedad notif.inicio.enabled, env NOTIF_INICIO_ENABLED).
  // Apagado, el job no consulta ni marca nada: los contratos quedan pendientes.
  @Value("${notif.inicio.enabled:true}")
  private boolean enabled;

  // Cada hora en punto (00 de cada hora), zona America/Lima.
  @Scheduled(cron = "0 0 * * * *", zone = "America/Lima")
  public void notificarInicioLabores() {
    if (!enabled) {
      logger.debug("[NotifInicio] Envío desactivado (notif.inicio.enabled=false).");
      return;
    }

    // Además del flag propio, manda el evento CORREO_PRONTO_INICIO del maestro 56.
    // Desactivado, tampoco se consulta ni se marca nada: los contratos siguen
    // pendientes y, si SP_BT_NOTIF_INICIO_LST todavía los devuelve (hitos D-2/D-0),
    // se notificarán en la primera ejecución tras reactivarlo. La decisión se
    // toma una vez para todo el lote.
    try (var decision = correoSwitch.decision(EventoNotificacion.PRONTO_INICIO)) {
      if (!decision.habilitado()) {
        logger.info("[NotifInicio] Envío omitido: {} desactivado.", EventoNotificacion.PRONTO_INICIO.clave());
        return;
      }
      logger.info("[NotifInicio] Inicio del job de notificación de inicio de labores (outsourcing).");

      List<InicioOutsourcingDTO> pendientes = repository.getPendientes();
      if (pendientes.isEmpty()) {
        logger.info("[NotifInicio] Sin contratos pendientes de notificar.");
        return;
      }
      logger.info("[NotifInicio] {} contrato(s) pendiente(s).", pendientes.size());

      int enviados = 0;
      for (InicioOutsourcingDTO dto : pendientes) {
        try {
          if (mailService.sendInicioOutsourcingNotification(dto)) {
            repository.marcar(dto.getIdContrato(), dto.getNuevoEstado());
            enviados++;
          }
        } catch (Exception e) {
          // Un fallo en un contrato no debe abortar el resto del lote.
          logger.error("[NotifInicio] Error notificando contrato {}: ", dto.getIdContrato(), e);
        }
      }

      logger.info("[NotifInicio] Job finalizado. Correos enviados/marcados: {}/{}.", enviados, pendientes.size());
    }
  }
}
