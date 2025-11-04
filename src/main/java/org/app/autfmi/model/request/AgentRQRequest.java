package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentRQRequest {

  private AgentDuracion duracionContrato;
  private Integer modalidad;
  private Boolean tieneDuracion;
  private String fechaVencimiento;
  private Integer idCliente;
  private String cliente;
  private String titulo;
  private String descripcion;
  private List<AgentContacto> lstContactos;
  private AgentDuracion duracion;
  private List<AgentVacante> lstVacantes;

}
