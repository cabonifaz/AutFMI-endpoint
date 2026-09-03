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
        (String) row.get("DURACION_CONTRATO"),
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
            (String) row.get("CARRERA"),
            (String) row.get("GRADO")));
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
            (String) row.get("ESTADO"),
            (String) row.get("DNI"),
            (String) row.get("SITUACION")));
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
   * Mapea el result set #9 a List<RequirementVacanteSummaryReport>
   */
  public static List<RequirementVacanteSummaryReport> mapVacanteSummary(List<Map<String, Object>> resultSet) {
    List<RequirementVacanteSummaryReport> summary = new ArrayList<>();
    if (resultSet != null) {
      for (Map<String, Object> row : resultSet) {
        summary.add(new RequirementVacanteSummaryReport(
            (Integer) row.get("ID_VACANTE"),
            (String) row.get("PERFIL"),
            (Integer) row.get("TOTAL_VACANTES")));
      }
    }
    return summary;
  }

  public static List<String> mapExtraMailList(List<Map<String, Object>> resultSet) {
    List<String> emails = new ArrayList<>();
    if (resultSet != null && !resultSet.isEmpty()) {
      for (Map<String, Object> row : resultSet) {
        String email = (String) row.get("CORREO");
        if (email != null && !email.isEmpty()) {
          emails.add(email);
        }
      }
    }
    return emails;
  }

  /**
   * Construye las vacantes completas consolidando resumen, habilidades y carreras
   * Protege contra NullPointerException
   */
  public static List<RequirementVacanteCompleteReport> buildCompleteVacantes(
      List<RequirementVacanteSummaryReport> summary,
      List<RequirementVacanteSkillReport> skills,
      List<RequirementVacanteCareerReport> careers) {

    List<RequirementVacanteCompleteReport> completeVacantes = new ArrayList<>();

    if (summary == null || summary.isEmpty()) {
      return completeVacantes;
    }

    for (RequirementVacanteSummaryReport summaryItem : summary) {
      if (summaryItem == null || summaryItem.getIdVacante() == null) {
        continue; // Protección contra nulos
      }

      RequirementVacanteCompleteReport completeVacante = new RequirementVacanteCompleteReport(
          summaryItem.getIdVacante(),
          summaryItem.getPerfil() != null ? summaryItem.getPerfil() : "Perfil no especificado",
          summaryItem.getTotalVacantes() != null ? summaryItem.getTotalVacantes() : 0);

      // Agregar habilidades relacionadas
      if (skills != null) {
        skills.stream()
            .filter(skill -> skill != null &&
                skill.getIdVacante() != null &&
                skill.getIdVacante().equals(summaryItem.getIdVacante()))
            .forEach(completeVacante::addHabilidad);
      }

      // Agregar carreras relacionadas
      if (careers != null) {
        careers.stream()
            .filter(career -> career != null &&
                career.getIdVacante() != null &&
                career.getIdVacante().equals(summaryItem.getIdVacante()))
            .forEach(completeVacante::addCarrera);
      }

      completeVacantes.add(completeVacante);
    }

    return completeVacantes;
  }

  /**
   * Método actualizado para mapear todos los result sets del SP
   * SP_REQUERIMIENTO_REPORTE_SEL
   * Incluye RS #9 (resumen de vacantes) y RS #10 (correos de notificación extra)
   * Protegido contra NullPointerException
   * 
   * @param extraMailSet Result Set #10 - Correos desde PARAMETROS ID_MAESTRO=42
   */
  public static RequirementReport mapCompleteReportV2(
      Map<String, Object> detailsRow,
      List<Map<String, Object>> contactsSet,
      List<Map<String, Object>> skillsSet,
      List<Map<String, Object>> careersSet,
      List<Map<String, Object>> postulantsSet,
      List<Map<String, Object>> managersSet,
      Map<String, Object> actionUserRow,
      List<Map<String, Object>> vacanteSummarySet,
      List<Map<String, Object>> extraMailSet) {

    RequirementReport report = new RequirementReport();

    // Mapear datos básicos
    report.setRequirementDetails(mapRequirementDetails(detailsRow));
    report.setContacts(mapContacts(contactsSet));
    report.setPostulants(mapPostulants(postulantsSet));
    report.setManagers(mapManagers(managersSet));
    report.setActionUser(mapActionUser(actionUserRow));

    // Mapear datos de vacantes
    List<RequirementVacanteSkillReport> skills = mapVacanteSkills(skillsSet);
    List<RequirementVacanteCareerReport> careers = mapVacanteCareers(careersSet);
    List<RequirementVacanteSummaryReport> summary = mapVacanteSummary(vacanteSummarySet);

    report.setVacanteSkills(skills);
    report.setVacanteCareers(careers);
    report.setVacanteSummary(summary);

    // Construir vacantes completas con relaciones
    report.setVacantesComplete(buildCompleteVacantes(summary, skills, careers));

    // Mapear correos de notificación extra (RS #10)
    report.setExtraMailList(mapExtraMailList(extraMailSet));

    return report;
  }

  /**
   * Sobrecarga para mantener compatibilidad con código existente
   * Sin el result set #10 de correos extra
   */
  public static RequirementReport mapCompleteReportV2(
      Map<String, Object> detailsRow,
      List<Map<String, Object>> contactsSet,
      List<Map<String, Object>> skillsSet,
      List<Map<String, Object>> careersSet,
      List<Map<String, Object>> postulantsSet,
      List<Map<String, Object>> managersSet,
      Map<String, Object> actionUserRow,
      List<Map<String, Object>> vacanteSummarySet) {

    return mapCompleteReportV2(detailsRow, contactsSet, skillsSet, careersSet,
        postulantsSet, managersSet, actionUserRow,
        vacanteSummarySet, null);
  }
}