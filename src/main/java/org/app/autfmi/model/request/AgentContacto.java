package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentContacto {
  private String nombres;
  private String apellidoPaterno;
  private String apellidoMaterno;
  private String correo;
  private String telefono;
  private String cargo;
}