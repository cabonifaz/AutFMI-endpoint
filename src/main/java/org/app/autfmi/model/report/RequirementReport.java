package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

import org.app.autfmi.model.response.BaseResponse;

@Data
@AllArgsConstructor
public class RequirementReport {

  private BaseResponse response;

  // Result Set #2: Detalles del RQ y cliente
  private RequirementDetailsReport requirementDetails;

  // Result Set #3: Contactos del cliente
  private List<RequirementContactReport> contacts;

  // Result Set #4: Vacantes con habilidades técnicas
  private List<RequirementVacanteSkillReport> vacanteSkills;

  // Result Set #5: Vacantes con carreras
  private List<RequirementVacanteCareerReport> vacanteCareers;

  // Result Set #6: Postulantes del RQ
  private List<RequirementPostulantReport> postulants;

  // Result Set #7: Gestores del cliente
  private List<RequirementManagerReport> managers;

  // Result Set #8: Usuario que realizó la acción
  private RequirementActionUserReport actionUser;

  // Result Set #9: Resumen de vacantes por perfil
  private List<RequirementVacanteSummaryReport> vacanteSummary;

  // Vacantes consolidadas con habilidades y carreras
  private List<RequirementVacanteCompleteReport> vacantesComplete;

  // Constructor de conveniencia para inicializar listas vacías
  public RequirementReport() {
    this.contacts = new java.util.ArrayList<>();
    this.vacanteSkills = new java.util.ArrayList<>();
    this.vacanteCareers = new java.util.ArrayList<>();
    this.postulants = new java.util.ArrayList<>();
    this.managers = new java.util.ArrayList<>();
    this.vacanteSummary = new java.util.ArrayList<>();
    this.vacantesComplete = new java.util.ArrayList<>();
  }
}
