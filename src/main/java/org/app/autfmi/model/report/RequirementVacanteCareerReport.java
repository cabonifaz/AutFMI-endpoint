package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementVacanteCareerReport {
  private Integer idVacante;
  private String carrera;
  private String grado;
}