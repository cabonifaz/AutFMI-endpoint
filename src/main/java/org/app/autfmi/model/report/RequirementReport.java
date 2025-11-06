package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class RequirementReport {

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

  // Constructor de conveniencia para inicializar listas vacías
  public RequirementReport() {
    this.contacts = new java.util.ArrayList<>();
    this.vacanteSkills = new java.util.ArrayList<>();
    this.vacanteCareers = new java.util.ArrayList<>();
    this.postulants = new java.util.ArrayList<>();
    this.managers = new java.util.ArrayList<>();
  }
}
