package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentSkill {
  private String skillName;
  private Integer yearsExp;
  private Boolean opcional;
}