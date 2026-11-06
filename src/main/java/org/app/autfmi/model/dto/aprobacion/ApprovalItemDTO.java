package org.app.autfmi.model.dto.aprobacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalItemDTO {
  private Integer idSolicitud;
  /** INGRESO | MOVIMIENTO | CESE */
  private String tipoSolicitud;
  /** PENDIENTE | APROBADA | RECHAZADA | ANULADA */
  private String estado;
  private Integer idCliente;
  private String cliente;
  private Integer idTalento;
  private String nombreTalento;
  private String resumen;
  private String usuarioSolicitante;
  /** yyyy-MM-dd HH:mm */
  private String fechaSolicitud;
}
