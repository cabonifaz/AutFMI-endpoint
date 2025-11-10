package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementVacanteSummaryReport {
  private Integer idVacante;
  private String perfil;
  private Integer totalVacantes;
}