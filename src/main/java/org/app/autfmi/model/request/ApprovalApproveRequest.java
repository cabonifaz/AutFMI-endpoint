package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Aprobación del flujo completo de una solicitud (FTBO-1124). */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalApproveRequest {
  /** Versión leída en el detalle: si otro gestor editó o aprobó antes, responde 409. */
  private Long version;
}
