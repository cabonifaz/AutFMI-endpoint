package org.app.autfmi.service.impl;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.ParametrosDTO;
import org.app.autfmi.service.IParametrosService;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.EventoNotificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Decide si un evento puede enviar correo según su fila del maestro 56. Se
 * consulta en cada operación (sin caché), así un cambio de NUM1 rige sin reiniciar.
 * Ante cualquier configuración inválida o error, ese evento no envía.
 */
@Service
@RequiredArgsConstructor
public class NotificacionCorreoSwitch {

  private static final Logger logger = LoggerFactory.getLogger(NotificacionCorreoSwitch.class);

  static final int NUM1_HABILITADO = 1;
  static final int NUM1_DESHABILITADO = 0;

  private final IParametrosService parametrosService;

  // Decisiones abiertas con decision(...) en el hilo actual.
  private final ThreadLocal<Map<EventoNotificacion, Boolean>> decisiones =
      ThreadLocal.withInitial(() -> new EnumMap<>(EventoNotificacion.class));

  public boolean permitido(EventoNotificacion evento) {
    Boolean fijada = decisiones.get().get(evento);
    return fijada != null ? fijada : consultar(evento);
  }

  /**
   * Consulta el evento una sola vez para todos los correos de una operación; los
   * envíos del mismo hilo reutilizan la decisión hasta cerrar el bloque.
   */
  public Decision decision(EventoNotificacion evento) {
    Map<EventoNotificacion, Boolean> abiertas = decisiones.get();
    Boolean fijada = abiertas.get(evento);
    if (fijada != null) {
      return new Decision(evento, fijada, false);
    }
    boolean habilitado = consultar(evento);
    abiertas.put(evento, habilitado);
    return new Decision(evento, habilitado, true);
  }

  public final class Decision implements AutoCloseable {
    private final EventoNotificacion evento;
    private final boolean habilitado;
    private final boolean propia;

    private Decision(EventoNotificacion evento, boolean habilitado, boolean propia) {
      this.evento = evento;
      this.habilitado = habilitado;
      this.propia = propia;
    }

    public boolean habilitado() {
      return habilitado;
    }

    @Override
    public void close() {
      if (propia) {
        Map<EventoNotificacion, Boolean> abiertas = decisiones.get();
        abiertas.remove(evento);
        if (abiertas.isEmpty()) {
          decisiones.remove();
        }
      }
    }
  }

  private boolean consultar(EventoNotificacion evento) {
    String maestro = Constante.MAESTRO_NOTIFICACION_CORREO;
    String clave = evento.clave();
    try {
      var response = parametrosService.listParametros(maestro);
      List<ParametrosDTO> filas = response != null && response.getListParametros() != null
          ? response.getListParametros()
          : List.of();

      // SP_PARAMETROS_LST solo devuelve filas vigentes.
      List<ParametrosDTO> delEvento = filas.stream()
          .filter(p -> p.getString1() != null && clave.equalsIgnoreCase(p.getString1().trim()))
          .toList();

      if (delEvento.isEmpty()) {
        logger.warn("Correo {} BLOQUEADO: el maestro {} no tiene su fila vigente.", clave, maestro);
        return false;
      }
      if (delEvento.size() > 1) {
        logger.warn("Correo {} BLOQUEADO: el maestro {} tiene {} filas vigentes y debe haber una sola.",
            clave, maestro, delEvento.size());
        return false;
      }

      Integer num1 = delEvento.get(0).getNum1();
      if (num1 == null) {
        logger.warn("Correo {} BLOQUEADO: NUM1 nulo en el maestro {}.", clave, maestro);
        return false;
      }
      if (num1 == NUM1_HABILITADO) {
        return true;
      }
      if (num1 == NUM1_DESHABILITADO) {
        // SP_PARAMETROS_LST también devuelve 0 cuando NUM1 es NULL en la tabla.
        logger.info("Correo {} desactivado en el maestro {} (NUM1 = 0).", clave, maestro);
        return false;
      }
      logger.warn("Correo {} BLOQUEADO: NUM1 = {} en el maestro {} (se esperaba {} o {}).",
          clave, num1, maestro, NUM1_DESHABILITADO, NUM1_HABILITADO);
      return false;
    } catch (Exception e) {
      logger.warn("Correo {} BLOQUEADO: no se pudo consultar el maestro {}: {}", clave, maestro, e.getMessage());
      return false;
    }
  }
}
