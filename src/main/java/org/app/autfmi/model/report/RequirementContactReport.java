package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementContactReport {
  private String nombreCompleto;
  private String correo;
  private String telefono;
  private String cargo;
}