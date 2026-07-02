package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentVacante {
  private String vacante;
  private Integer cantidad;
  private List<AgentSkill> skills;
  private List<AgentCareer> careers;
}