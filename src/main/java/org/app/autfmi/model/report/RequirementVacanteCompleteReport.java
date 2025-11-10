package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementVacanteCompleteReport {
  private Integer idVacante;
  private String perfil;
  private Integer totalVacantes;
  private List<RequirementVacanteSkillReport> habilidades;
  private List<RequirementVacanteCareerReport> carreras;

  // Constructor que inicializa las listas para evitar NullPointerException
  public RequirementVacanteCompleteReport(Integer idVacante, String perfil, Integer totalVacantes) {
    this.idVacante = idVacante;
    this.perfil = perfil;
    this.totalVacantes = totalVacantes;
    this.habilidades = new ArrayList<>();
    this.carreras = new ArrayList<>();
  }

  // Métodos de conveniencia para agregar datos con protección null
  public void addHabilidad(RequirementVacanteSkillReport habilidad) {
    if (this.habilidades == null) {
      this.habilidades = new ArrayList<>();
    }
    if (habilidad != null) {
      this.habilidades.add(habilidad);
    }
  }

  public void addCarrera(RequirementVacanteCareerReport carrera) {
    if (this.carreras == null) {
      this.carreras = new ArrayList<>();
    }
    if (carrera != null) {
      this.carreras.add(carrera);
    }
  }
}