package org.app.autfmi.model.dto.aprobacion;

import org.springframework.http.HttpStatus;


public enum ApprovalOutcome {
  OK(HttpStatus.OK),
  SIN_PERMISO(HttpStatus.FORBIDDEN),
  NO_ENCONTRADO(HttpStatus.NOT_FOUND),
  PARAMETRO_INVALIDO(HttpStatus.BAD_REQUEST),
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
