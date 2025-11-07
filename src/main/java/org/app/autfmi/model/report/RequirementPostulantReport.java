package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementPostulantReport {
  private String nombreCompleto;
  private String correo;
  private String celular;
  private String perfil;
  private String estado;
  private String dni;
  private String situacion;
}