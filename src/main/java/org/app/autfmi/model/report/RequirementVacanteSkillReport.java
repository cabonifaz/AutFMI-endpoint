package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementVacanteSkillReport {
  private Integer idVacante;
  private String perfil;
  private String habilidad;
  private Integer aExp; // años de experiencia
}