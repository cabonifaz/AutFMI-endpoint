package org.app.autfmi.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.app.autfmi.model.dto.aprobacion.ApprovalEditableFieldDTO;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Formularios de las solicitudes de aprobación (FTBO-1124): de qué clase es
 * cada tipo, qué campos puede editar el gestor y cómo se validan los cambios.
 * Identificadores (talento, cliente, área, contrato, RQ, motivo, modalidad) no
 * son editables: cambiarlos sería otra solicitud.
 */
public final class ApprovalFormPolicy {

  public static final String TEXTO = "TEXTO";
  public static final String NUMERO = "NUMERO";
  public static final String FECHA = "FECHA";
  public static final String SI_NO = "SI_NO";

  private static final int MAX_TEXTO = 500;

  /** Etiquetas de todos los campos conocidos, en el orden en que se muestran. */
  private static final Map<String, String> ETIQUETAS = new LinkedHashMap<>();

  static {
    ETIQUETAS.put("nombres", "Nombres");
    ETIQUETAS.put("apellidoPaterno", "Apellido paterno");
    ETIQUETAS.put("apellidoMaterno", "Apellido materno");
    ETIQUETAS.put("cliente", "Cliente");
    ETIQUETAS.put("area", "Área");
    ETIQUETAS.put("movArea", "Área destino");
    ETIQUETAS.put("cargo", "Cargo");
    ETIQUETAS.put("puesto", "Puesto");
    ETIQUETAS.put("horario", "Horario");
    ETIQUETAS.put("ubicacion", "Ubicación");
    ETIQUETAS.put("fchInicioContrato", "Inicio de contrato");
    ETIQUETAS.put("fchTerminoContrato", "Término de contrato");
    ETIQUETAS.put("fchMovimiento", "Fecha del movimiento");
    ETIQUETAS.put("fchCese", "Fecha de cese");
    ETIQUETAS.put("fchDevolucionEquipo", "Devolución de equipo");
    ETIQUETAS.put("tiempoContrato", "Tiempo de contrato");
    ETIQUETAS.put("proyectoServicio", "Proyecto / servicio");
    ETIQUETAS.put("objetoContrato", "Objeto del contrato");
    ETIQUETAS.put("remuneracion", "Remuneración");
    ETIQUETAS.put("montoBase", "Monto base");
    ETIQUETAS.put("montoMovilidad", "Movilidad");
    ETIQUETAS.put("montoTrimestral", "Bono trimestral");
    ETIQUETAS.put("montoSemestral", "Bono semestral");
    ETIQUETAS.put("tieneEquipo", "Equipo propio");
    ETIQUETAS.put("sedeDeclarar", "Sede a declarar");
  }

  private static final Map<String, Map<String, String>> EDITABLES = Map.of(
      "INGRESO", campos(
          "cargo", TEXTO, "horario", TEXTO, "ubicacion", TEXTO,
          "fchInicioContrato", FECHA, "fchTerminoContrato", FECHA,
          "proyectoServicio", TEXTO, "objetoContrato", TEXTO,
          "remuneracion", NUMERO, "montoBase", NUMERO, "montoMovilidad", NUMERO,
          "montoTrimestral", NUMERO, "montoSemestral", NUMERO, "tieneEquipo", SI_NO),
      "MOVIMIENTO", campos(
          "puesto", TEXTO, "horario", TEXTO, "fchMovimiento", FECHA,
          "fchInicioContrato", FECHA, "fchTerminoContrato", FECHA,
          "proyectoServicio", TEXTO, "objetoContrato", TEXTO,
          "montoBase", NUMERO, "montoMovilidad", NUMERO,
          "montoTrimestral", NUMERO, "montoSemestral", NUMERO),
      "CESE", campos(
          "fchCese", FECHA, "fchDevolucionEquipo", FECHA));

  private ApprovalFormPolicy() {
  }

  /** Formulario validado y descripción de lo que cambió, para el historial. */
  public record AppliedChanges(String datosFormulario, String detalle, boolean cambio) {
  }

  public static Class<?> formClass(String tipo) {
    return switch (tipo == null ? "" : tipo) {
      case "INGRESO" -> EmployeeEntryRequest.class;
      case "MOVIMIENTO" -> EmployeeMovementRequest.class;
      case "CESE" -> EmployeeContractEndRequest.class;
      default -> throw new IllegalArgumentException("Tipo de solicitud no válido.");
    };
  }

  public static List<ApprovalEditableFieldDTO> editableFields(String tipo) {
    Map<String, String> campos = EDITABLES.getOrDefault(tipo, Map.of());
    List<ApprovalEditableFieldDTO> result = new ArrayList<>();
    campos.forEach((campo, tipoCampo) -> result.add(new ApprovalEditableFieldDTO(campo, etiqueta(campo), tipoCampo)));
    return result;
  }

  public static String etiqueta(String campo) {
    return ETIQUETAS.getOrDefault(campo, campo);
  }

  /** Filas etiqueta/valor de un formulario para el correo, solo campos con valor. */
  public static List<Map<String, String>> resumenFormulario(ObjectMapper mapper, String datosFormulario) {
    List<Map<String, String>> filas = new ArrayList<>();
    try {
      JsonNode node = mapper.readTree(datosFormulario);
      ETIQUETAS.forEach((campo, etiqueta) -> {
        JsonNode valor = node.get(campo);
        if (valor != null && !valor.isNull() && !valor.asText().isBlank()) {
          filas.add(Map.of("etiqueta", etiqueta, "valor", texto(campo, valor)));
        }
      });
    } catch (Exception e) {
      // Sin tabla: el correo igual lleva el enlace a la solicitud.
    }
    return filas;
  }

  /**
   * Aplica los cambios del gestor sobre el formulario guardado. Rechaza campos
   * no editables y valores con otro tipo; el resultado debe seguir siendo un
   * formulario válido de su clase.
   *
   * @throws IllegalArgumentException con un mensaje para el usuario.
   */
  public static AppliedChanges apply(ObjectMapper mapper, String tipo, String datosFormulario,
      Map<String, Object> cambios) {
    Map<String, String> editables = EDITABLES.get(tipo);
    if (editables == null) {
      throw new IllegalArgumentException("Tipo de solicitud no válido.");
    }
    if (cambios == null || cambios.isEmpty()) {
      throw new IllegalArgumentException("No hay cambios que guardar.");
    }

    ObjectNode form;
    try {
      JsonNode parsed = mapper.readTree(datosFormulario);
      if (!(parsed instanceof ObjectNode objectNode)) {
        throw new IllegalArgumentException("El formulario guardado no es válido.");
      }
      form = objectNode;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("El formulario guardado no es válido.");
    }

    List<String> detalle = new ArrayList<>();
    for (Map.Entry<String, Object> cambio : cambios.entrySet()) {
      String campo = cambio.getKey();
      String tipoCampo = editables.get(campo);
      if (tipoCampo == null) {
        throw new IllegalArgumentException("El campo '" + etiqueta(campo) + "' no es editable.");
      }
      JsonNode nuevo = validar(mapper, campo, tipoCampo, cambio.getValue());
      JsonNode anterior = form.get(campo);
      if (!Objects.equals(normalizado(anterior), normalizado(nuevo))) {
        detalle.add(etiqueta(campo) + ": " + mostrar(campo, anterior) + " → " + mostrar(campo, nuevo));
        form.set(campo, nuevo);
      }
    }

    validarFechas(form, "fchInicioContrato", "fchTerminoContrato");

    try {
      // Debe seguir siendo un formulario de su clase (tipos y nombres de campo).
      mapper.treeToValue(form, formClass(tipo));
      return new AppliedChanges(mapper.writeValueAsString(form), String.join("; ", detalle), !detalle.isEmpty());
    } catch (Exception e) {
      throw new IllegalArgumentException("Los datos del formulario no son válidos.");
    }
  }

  private static JsonNode validar(ObjectMapper mapper, String campo, String tipoCampo, Object valor) {
    String etiqueta = etiqueta(campo);
    if (valor == null) {
      if (SI_NO.equals(tipoCampo)) {
        throw new IllegalArgumentException("Indique Sí o No en '" + etiqueta + "'.");
      }
      return mapper.nullNode();
    }
    switch (tipoCampo) {
      case TEXTO -> {
        if (!(valor instanceof String texto)) {
          throw new IllegalArgumentException("'" + etiqueta + "' debe ser un texto.");
        }
        String limpio = texto.trim();
        if (limpio.length() > MAX_TEXTO) {
          throw new IllegalArgumentException("'" + etiqueta + "' admite hasta " + MAX_TEXTO + " caracteres.");
        }
        return mapper.getNodeFactory().textNode(limpio);
      }
      case NUMERO -> {
        if (!(valor instanceof Number numero) || numero.doubleValue() < 0
            || Double.isNaN(numero.doubleValue()) || Double.isInfinite(numero.doubleValue())) {
          throw new IllegalArgumentException("'" + etiqueta + "' debe ser un número mayor o igual a 0.");
        }
        return mapper.getNodeFactory().numberNode(numero.doubleValue());
      }
      case FECHA -> {
        if (!(valor instanceof String fecha) || fecha.isBlank()) {
          throw new IllegalArgumentException("'" + etiqueta + "' debe ser una fecha.");
        }
        try {
          LocalDate.parse(fecha.trim());
        } catch (DateTimeParseException e) {
          throw new IllegalArgumentException("'" + etiqueta + "' debe tener el formato aaaa-mm-dd.");
        }
        return mapper.getNodeFactory().textNode(fecha.trim());
      }
      case SI_NO -> {
        if (!(valor instanceof Number numero) || (numero.intValue() != 0 && numero.intValue() != 1)
            || numero.doubleValue() != numero.intValue()) {
          throw new IllegalArgumentException("Indique Sí o No en '" + etiqueta + "'.");
        }
        return mapper.getNodeFactory().numberNode(numero.intValue());
      }
      default -> throw new IllegalArgumentException("El campo '" + etiqueta + "' no es editable.");
    }
  }

  private static void validarFechas(ObjectNode form, String inicio, String termino) {
    JsonNode a = form.get(inicio);
    JsonNode b = form.get(termino);
    if (a == null || b == null || a.isNull() || b.isNull() || a.asText().isBlank() || b.asText().isBlank()) {
      return;
    }
    try {
      if (LocalDate.parse(b.asText()).isBefore(LocalDate.parse(a.asText()))) {
        throw new IllegalArgumentException("El término de contrato no puede ser anterior al inicio.");
      }
    } catch (DateTimeParseException e) {
      // Formato heredado del formulario original: lo valida el SP al ejecutar.
    }
  }

  private static String normalizado(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return node.isNumber() ? String.valueOf(node.doubleValue()) : node.asText();
  }

  private static String mostrar(String campo, JsonNode node) {
    return node == null || node.isNull() || node.asText().isBlank() ? "(vacío)" : texto(campo, node);
  }

  private static String texto(String campo, JsonNode node) {
    if ("tieneEquipo".equals(campo) || "declararSunat".equals(campo)) {
      return node.asInt() == 1 ? "Sí" : "No";
    }
    if (node.isNumber() && node.doubleValue() == Math.rint(node.doubleValue())) {
      return String.valueOf(node.longValue());
    }
    return node.asText();
  }

  private static Map<String, String> campos(String... pares) {
    Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < pares.length; i += 2) {
      map.put(pares[i], pares[i + 1]);
    }
    return map;
  }
}
