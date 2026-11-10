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
  /** Fecha de solicitud, yyyy-MM-dd, extremos inclusivos. */
  private String fechaDesde;
  private String fechaHasta;
  public ApprovalListRequest(Integer idCliente, String tipoSolicitud, String estado, String busqueda, Integer nPag, Integer cPag) {
    this(idCliente, tipoSolicitud, estado, busqueda, null, null, nPag, cPag);
  }
  private Integer nPag;
  private Integer cPag;
}
