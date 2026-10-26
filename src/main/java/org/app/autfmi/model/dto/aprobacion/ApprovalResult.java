package org.app.autfmi.model.dto.aprobacion;

import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.OperationResult;

/** Resultado de una consulta de la bandeja: el cuerpo de la respuesta y su código. */
public record ApprovalResult<T>(ApprovalOutcome outcome, OperationResult<T> body) {

  public static <T> ApprovalResult<T> of(ApprovalOutcome outcome, int idTipoMensaje, String mensaje, T data) {
    return new ApprovalResult<>(outcome, new OperationResult<>(new BaseResponse(idTipoMensaje, mensaje), data));
  }

  public static <T> ApprovalResult<T> error(String mensaje) {
    return of(ApprovalOutcome.ERROR, 3, mensaje, null);
  }
}
