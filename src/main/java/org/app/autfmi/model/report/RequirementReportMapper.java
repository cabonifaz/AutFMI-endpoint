package org.app.autfmi.model.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RequirementReportMapper {

  /**
   * Mapea el result set #2 a RequirementDetailsReport
   */
  public static RequirementDetailsReport mapRequirementDetails(Map<String, Object> row) {
    if (row == null)
      return null;

    return new RequirementDetailsReport(
        (String) row.get("TITULO"),
        (String) row.get("DESCRIPCION"),
        (String) row.get("CODIGO_RQ"),
        (java.util.Date) row.get("FECHA_SOLICITUD"),
        (java.util.Date) row.get("FECHA_VENCIMIENTO"),
        (String) row.get("ESTADO_RQ"),
        (String) row.get("MODALIDAD_RQ"),
        (String) row.get("DURACION_RQ"),
        (String) row.get("CLIENTE"));
  }

  /**
   * Mapea el result set #3 a List<RequirementContactReport>
   */
  public static List<RequirementContactReport> mapContacts(List<Map<String, Object>> resultSet) {
    List<RequirementContactReport> contacts = new ArrayList<>();
    if (resultSet != null) {
      for (Map<String, Object> row : resultSet) {
        contacts.add(new RequirementContactReport(
            (String) row.get("NOMBRE_COMPLETO"),
            (String) row.get("CORREO"),
            (String) row.get("TELEFONO"),
            (String) row.get("CARGO")));
      }
    }
    return contacts;
  }

  /**
   * Mapea el result set #4 a List<RequirementVacanteSkillReport>
   */
  public static List<RequirementVacanteSkillReport> mapVacanteSkills(List<Map<String, Object>> resultSet) {
    List<RequirementVacanteSkillReport> skills = new ArrayList<>();
    if (resultSet != null) {
      for (Map<String, Object> row : resultSet) {
        skills.add(new RequirementVacanteSkillReport(
            (Integer) row.get("ID_VACANTE"),
            (String) row.get("PERFIL"),
            (String) row.get("HABILIDAD"),
            (Integer) row.get("A_EXP")));
      }
    }
    return skills;
  }

  /**
   * Mapea el result set #5 a List<RequirementVacanteCareerReport>
   */
  public static List<RequirementVacanteCareerReport> mapVacanteCareers(List<Map<String, Object>> resultSet) {
    List<RequirementVacanteCareerReport> careers = new ArrayList<>();
    if (resultSet != null) {
      for (Map<String, Object> row : resultSet) {
        careers.add(new RequirementVacanteCareerReport(
            (Integer) row.get("ID_VACANTE"),
            (String) row.get("CARRERA")));
      }
    }
    return careers;
  }

  /**
   * Mapea el result set #6 a List<RequirementPostulantReport>
   */
  public static List<RequirementPostulantReport> mapPostulants(List<Map<String, Object>> resultSet) {
    List<RequirementPostulantReport> postulants = new ArrayList<>();
    if (resultSet != null) {
      for (Map<String, Object> row : resultSet) {
        postulants.add(new RequirementPostulantReport(
            (String) row.get("NOMBRE_COMPLETO"),
            (String) row.get("CORREO"),
            (String) row.get("CELULAR"),
            (String) row.get("PERFIL"),
            (String) row.get("ESTADO")));
      }
    }
    return postulants;
  }

  /**
   * Mapea el result set #7 a List<RequirementManagerReport>
   */
  public static List<RequirementManagerReport> mapManagers(List<Map<String, Object>> resultSet) {
    List<RequirementManagerReport> managers = new ArrayList<>();
    if (resultSet != null) {
      for (Map<String, Object> row : resultSet) {
        managers.add(new RequirementManagerReport(
            (String) row.get("NOMBRES"),
            (String) row.get("EMAIL")));
      }
    }
    return managers;
  }

  /**
   * Mapea el result set #8 a RequirementActionUserReport
   */
  public static RequirementActionUserReport mapActionUser(Map<String, Object> row) {
    if (row == null)
      return null;

    return new RequirementActionUserReport(
        (String) row.get("USUARIO"),
        (String) row.get("CORREO"));
  }

  /**
   * Método de conveniencia para mapear todos los result sets a un
   * RequirementReport completo
   */
  public static RequirementReport mapCompleteReport(
      Map<String, Object> detailsRow,
      List<Map<String, Object>> contactsSet,
      List<Map<String, Object>> skillsSet,
      List<Map<String, Object>> careersSet,
      List<Map<String, Object>> postulantsSet,
      List<Map<String, Object>> managersSet,
      Map<String, Object> actionUserRow) {

    RequirementReport report = new RequirementReport();

    report.setRequirementDetails(mapRequirementDetails(detailsRow));
    report.setContacts(mapContacts(contactsSet));
    report.setVacanteSkills(mapVacanteSkills(skillsSet));
    report.setVacanteCareers(mapVacanteCareers(careersSet));
    report.setPostulants(mapPostulants(postulantsSet));
    report.setManagers(mapManagers(managersSet));
    report.setActionUser(mapActionUser(actionUserRow));

    return report;
  }
}