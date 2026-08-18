package org.app.autfmi.service.impl;

import java.util.List;

import org.app.autfmi.model.dto.InicioOutsourcingDTO;
import org.app.autfmi.repository.InicioNotificationRepository;
import org.app.autfmi.service.IMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InicioOutsourcingScheduler {

  private final Logger logger = LoggerFactory.getLogger(InicioOutsourcingScheduler.class);

  private final InicioNotificationRepository repository;
  private final IMailService mailService;

  // Cada hora en punto (00 de cada hora), zona America/Lima.
  @Scheduled(cron = "0 0 * * * *", zone = "America/Lima")
  public void notificarInicioLabores() {
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
