package org.app.autfmi.service.impl;

import java.util.List;

import org.app.autfmi.model.dto.ParametrosDTO;
import org.app.autfmi.service.IParametrosService;
import org.app.autfmi.util.Constante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class NotificacionCorreoSwitch {

  private static final Logger logger = LoggerFactory.getLogger(NotificacionCorreoSwitch.class);

  /** Valores válidos de NUM1 en la fila CORREO_GLOBAL. */
  static final int NUM1_HABILITADO = 1;
  static final int NUM1_DESHABILITADO = 0;

  private final IParametrosService parametrosService;

  /** Decisión fijada por {@link #decisionUnica()} para el hilo actual. */
  private final ThreadLocal<Boolean> decisionEnCurso = new ThreadLocal<>();

 
  public boolean correosHabilitados() {
    Boolean fijada = decisionEnCurso.get();
    return fijada != null ? fijada : consultar();
  }


  public boolean envioPermitido(String asunto) {
    if (correosHabilitados()) {
      return true;
    }
    logger.info("Correo '{}' omitido: notificaciones por correo desactivadas (maestro {}).",
        asunto, Constante.MAESTRO_NOTIFICACION_CORREO);
    return false;
  }

  public Decision decisionUnica() {
    Boolean fijada = decisionEnCurso.get();
    if (fijada != null) {
      return new Decision(fijada, false);
    }
    boolean habilitado = consultar();
    decisionEnCurso.set(habilitado);
    return new Decision(habilitado, true);
  }

  /** Decisión del interruptor para una operación. */
  public final class Decision implements AutoCloseable {
    private final boolean habilitado;
    private final boolean propia;

    private Decision(boolean habilitado, boolean propia) {
      this.habilitado = habilitado;
      this.propia = propia;
    }

    public boolean habilitado() {
      return habilitado;
    }

    @Override
    public void close() {
      if (propia) {
        decisionEnCurso.remove();
      }
    }
  }

  private boolean consultar() {
    String maestro = Constante.MAESTRO_NOTIFICACION_CORREO;
    String clave = Constante.PARAM_CORREO_GLOBAL;
    try {
      var response = parametrosService.listParametros(maestro);
      List<ParametrosDTO> filas = response != null && response.getListParametros() != null
          ? response.getListParametros()
          : List.of();

      List<ParametrosDTO> globales = filas.stream()
          .filter(p -> p.getString1() != null && clave.equalsIgnoreCase(p.getString1().trim()))
          .toList();

      if (globales.isEmpty()) {
        logger.warn("Correos BLOQUEADOS: el maestro {} no tiene la fila {} vigente.", maestro, clave);
        return false;
      }
      if (globales.size() > 1) {
        logger.warn("Correos BLOQUEADOS: el maestro {} tiene {} filas {} y debe haber una sola.",
            maestro, globales.size(), clave);
        return false;
      }

      Integer num1 = globales.get(0).getNum1();
      if (num1 == null) {
        logger.warn("Correos BLOQUEADOS: la fila {} del maestro {} tiene NUM1 nulo.", clave, maestro);
        return false;
      }
      if (num1 == NUM1_HABILITADO) {
        return true;
      }
      if (num1 == NUM1_DESHABILITADO) {
        // SP_PARAMETROS_LST también devuelve 0 cuando NUM1 es NULL en la tabla.
        logger.debug("Correos desactivados por el maestro {} ({} NUM1 = {}).", maestro, clave, num1);
        return false;
      }
      logger.warn("Correos BLOQUEADOS: la fila {} del maestro {} tiene NUM1 = {} (se esperaba {} o {}).",
          clave, maestro, num1, NUM1_DESHABILITADO, NUM1_HABILITADO);
      return false;
    } catch (Exception e) {
      logger.warn("Correos BLOQUEADOS: no se pudo consultar el maestro {}: {}", maestro, e.getMessage());
      return false;
    }
  }
}
