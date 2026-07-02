package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewListRequest {
  private Integer nPag;
  private String busqueda;
  private Integer idCliente;
  private Integer idEstado;
  private Integer idEtapa;
  private String fecha;
}
