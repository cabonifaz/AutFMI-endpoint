package org.app.autfmi.model.dto.aprobacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cliente asignado al gestor: opción del filtro Cliente de la bandeja. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalClientDTO {
  private Integer idCliente;
  private String razonSocial;
}
