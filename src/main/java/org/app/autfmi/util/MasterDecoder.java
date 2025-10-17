package org.app.autfmi.util;

class RQDurationMaster {

  public static final int DAYS = 1;
  public static final int WEEKS = 2;
  public static final int MONTHS = 3;

}

class RQContractTypeMaster {

  public static final int IN_PERSON = 1;
  public static final int REMOTE = 2;
  public static final int HYBRID = 3;

}

class RQModalityFacturationMaster {

  public static final int LOCACION_SERVICIOS = 1;
  public static final int PLANILLA_GENERAL_COMPLETA = 2;
  public static final int PLANILLA_GENERAL_PARCIAL = 3;
  public static final int PRACTICAS_PREPROFESIONALES = 4;
  public static final int PRACTICAS_PROFESIONALES = 5;
}

class RQStateMaster {

  public static final int REGISTRADO = 1;
  public static final int ASIGNADO = 2;
  public static final int ATENDIDO = 3;
  public static final int EN_SELECCION = 4;
  public static final int EN_PRODUCCION = 5;
  public static final int PERDIDO = 6;
  public static final int TERMINADO = 7;
  public static final int CANCELADO = 8;

}

public class MasterDecoder {

  public static String decodeDuration(Integer idDuration) {

    switch (idDuration) {

      case RQDurationMaster.DAYS:
        return "Días";

      case RQDurationMaster.WEEKS:
        return "Semanas";

      case RQDurationMaster.MONTHS:
        return "Meses";

      default:
        return "Desconocida";
    }
  }

  public static String decodeContractType(Integer idContractType) {

    switch (idContractType) {

      case RQContractTypeMaster.IN_PERSON:
        return "Presencial";

      case RQContractTypeMaster.REMOTE:
        return "Remoto";

      case RQContractTypeMaster.HYBRID:
        return "Híbrido";

      default:
        return "Desconocida";
    }
  }

  public static String decodeRQState(Integer state) {
    switch (state) {
      case RQStateMaster.REGISTRADO:
        return "Registrado";
      case RQStateMaster.ASIGNADO:
        return "Asignado";
      case RQStateMaster.ATENDIDO:
        return "Atendido";
      case RQStateMaster.EN_SELECCION:
        return "En selección";
      case RQStateMaster.EN_PRODUCCION:
        return "En producción";
      case RQStateMaster.PERDIDO:
        return "Perdido";
      case RQStateMaster.TERMINADO:
        return "Terminado";
      case RQStateMaster.CANCELADO:
        return "Cancelado";
      default:
        return "Desconocido";
    }
  }

  public static java.util.List<String> decodeModalityFacturationList(String modalitiesCsv) {
    java.util.List<String> result = new java.util.ArrayList<>();
    if (modalitiesCsv == null || modalitiesCsv.trim().isEmpty()) {
      return result;
    }

    String[] parts = modalitiesCsv.split(",");

    for (String part : parts) {
      String token = part.trim();
      if (token.isEmpty()) {
        continue;
      }

      try {
        int id = Integer.parseInt(token);
        String label = null;

        switch (id) {
          case RQModalityFacturationMaster.LOCACION_SERVICIOS:
            label = "Locación de servicios";
            break;
          case RQModalityFacturationMaster.PLANILLA_GENERAL_COMPLETA:
            label = "Planilla - Reg. general";
            break;
          case RQModalityFacturationMaster.PLANILLA_GENERAL_PARCIAL:
            label = "Planilla - Tiempo parcial";
            break;
          case RQModalityFacturationMaster.PRACTICAS_PREPROFESIONALES:
            label = "Prácticas Pre-profesionales";
            break;
          case RQModalityFacturationMaster.PRACTICAS_PROFESIONALES:
            label = "Prácticas Profesionales";
            break;
          default:
            // id desconocido -> ignorar
        }

        if (label != null) {
          result.add(label);
        }
      } catch (NumberFormatException ignored) {
        // token no es un número -> ignorar
      }
    }

    return result;
  }

}
