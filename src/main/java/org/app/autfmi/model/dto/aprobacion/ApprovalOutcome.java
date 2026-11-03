package org.app.autfmi.model.dto.aprobacion;

import org.springframework.http.HttpStatus;


public enum ApprovalOutcome {
  OK(HttpStatus.OK),
  SIN_PERMISO(HttpStatus.FORBIDDEN),
  NO_ENCONTRADO(HttpStatus.NOT_FOUND),
  PARAMETRO_INVALIDO(HttpStatus.BAD_REQUEST),
  // FTBO-1124: la solicitud cambió de estado o de versión (otro gestor aprobó o editó).
  CONFLICTO(HttpStatus.CONFLICT),
  // FTBO-1124: la operación aprobada no pudo ejecutarse; la solicitud vuelve a PENDIENTE.
  NO_PROCESABLE(HttpStatus.UNPROCESSABLE_ENTITY),
  // FTBO-1124: aprobada, pero no se pudo confirmar si la operación se registró
  // (error de conexión o tiempo agotado). Queda APROBADA hasta verificarla.
  VERIFICACION_PENDIENTE(HttpStatus.ACCEPTED),
  ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

  private final HttpStatus httpStatus;

  ApprovalOutcome(HttpStatus httpStatus) {
    this.httpStatus = httpStatus;
  }

  public HttpStatus httpStatus() {
    return httpStatus;
  }

  public static ApprovalOutcome fromCode(Object code) {
    if (code instanceof String value) {
      for (ApprovalOutcome outcome : values()) {
        if (outcome.name().equals(value.trim())) {
          return outcome;
        }
      }
    }
    return ERROR;
  }
}
