package org.app.autfmi.model.dto.aprobacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Detalle de una solicitud (SP_FMI_SOLICITUD_APROBACION_SEL #result-set-2). */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalDetailDTO {
  private Integer idSolicitud;
  private String tipoSolicitud;
  private String estado;
  private Integer idCliente;
  private String cliente;
  private Integer idTalento;
  private String nombreTalento;
  private String resumen;
  /** Instantánea del formulario enviado a aprobación, en JSON. */
  private String datosFormulario;
  private String usuarioSolicitante;
  private String fechaSolicitud;
  private String usuarioModificacion;
  private String fechaModificacion;
}
