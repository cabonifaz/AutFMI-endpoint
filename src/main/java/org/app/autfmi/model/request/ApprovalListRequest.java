package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalListRequest {
  private Integer idCliente;
  /** INGRESO | MOVIMIENTO | CESE */
  private String tipoSolicitud;
  /** PENDIENTE | APROBADA | RECHAZADA | ANULADA */
  private String estado;
  /** Número exacto de solicitud, o texto del nombre del talento o del resumen. */
  private String busqueda;
  private Integer nPag;
  private Integer cPag;
}
