package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentRQRequest {

  private String codigoRQ;
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

  // Requisitos de ingreso extraídos del correo por la IA (n8n): 1 = requiere,
  // 0 = no requiere, null = el correo no lo menciona. Viajan en AGENT_JSON.
  private Integer emo;
  private Integer sctr;
  private Integer asignacionLaptop;

}
