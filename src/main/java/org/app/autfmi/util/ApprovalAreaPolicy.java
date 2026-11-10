package org.app.autfmi.util;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.*;
import org.app.autfmi.model.dto.aprobacion.ApprovalEditableFieldDTO;

/** Formulario por área. Solo expone campos de negocio; los identificadores no son editables. */
public final class ApprovalAreaPolicy {

  public static final List<String> AREAS = List.of(
    "FMI",
    "EQUIPO",
    "CREDENCIALES"
  );

  private ApprovalAreaPolicy() {}

  public static boolean applies(String type, JsonNode form, String area) {
    return switch (area) {
      case "FMI" -> Set.of("INGRESO", "MOVIMIENTO", "CESE").contains(type);
      case "EQUIPO" -> "ASIGNACION_EQUIPO".equals(type) ||
        ("CESE".equals(type) &&
          !form.path("fchDevolucionEquipo").asText("").isBlank()) ||
        ("INGRESO".equals(type) && equipment(form).isObject());
      case "CREDENCIALES" -> Set.of("INGRESO", "CESE", "CREDENCIALES").contains(
        type
      );
      default -> false;
    };
  }

  public static JsonNode equipment(JsonNode form) {
    if (form.path("seleccion").path("solicitudEquipo").isObject()) return form
      .path("seleccion")
      .path("solicitudEquipo");
    return form.path("solicitudEquipo");
  }

  public static Map<String, Object> data(
    ObjectMapper mapper,
    String type,
    JsonNode form,
    String area
  ) {
    JsonNode source = source(type, form, area);
    Map<String, Object> values = new LinkedHashMap<>();
    for (var field : fields(type, area)) {
      JsonNode value = source.get(field.campo());
      if (value != null && !value.isNull()) values.put(
        field.campo(),
        mapper.convertValue(value, Object.class)
      );
    }
    if ("FMI".equals(area)) for (String key : List.of(
      "nombres",
      "apellidoPaterno",
      "apellidoMaterno",
      "cliente",
      "area",
      "movArea"
    )) {
      if (form.hasNonNull(key)) values.putIfAbsent(key, form.get(key).asText());
    }
    if ("EQUIPO".equals(area) && source.hasNonNull("tipoEquipo")) values.put(
      "tipoEquipo",
      source.get("tipoEquipo").asText()
    );
    if ("EQUIPO".equals(area) && !"CESE".equals(type)) {
      for (String key : List.of(
        "anexo",
        "celular",
        "internetMovil",
        "bitCelular",
        "bitInternetMovil"
      ))
        if (source.hasNonNull(key)) values.put(
          key,
          mapper.convertValue(source.get(key), Object.class)
        );
      if (source.path("lstSoftware").isArray()) {
        List<String> software = new ArrayList<>();
        for (JsonNode item : source.path("lstSoftware"))
          software.add(
            item.path("producto").asText("") +
              " " +
              item.path("prodVersion").asText("")
          );
        if (!software.isEmpty()) values.put(
          "software",
          String.join(", ", software)
        );
      }
    }
    if ("CREDENCIALES".equals(area) && !"CREDENCIALES".equals(type)) values.put(
      "accion",
      "CESE".equals(type) ? "Baja de credenciales" : "Creación de credenciales"
    );
    if("CREDENCIALES".equals(area)&&"CREDENCIALES".equals(type))values.put("accion","CAMBIO".equals(form.path("accion").asText())?"Cambio de credenciales":"Creación de credenciales");
    return values;
  }

  public static JsonNode source(String type, JsonNode form, String area) {
    if ("EQUIPO".equals(area) && "INGRESO".equals(type)) return equipment(form);
    if (
      "CREDENCIALES".equals(area) && !"CREDENCIALES".equals(type)
    ) return form.path("credenciales");
    return form;
  }

  public static List<ApprovalEditableFieldDTO> fields(
    String type,
    String area
  ) {
    if ("FMI".equals(area)) return ApprovalFormPolicy.editableFields(type)
      .stream()
      .filter(f -> !"fchDevolucionEquipo".equals(f.campo()))
      .toList();
    if ("EQUIPO".equals(area)) {
      if ("CESE".equals(type)) return List.of(
        new ApprovalEditableFieldDTO(
          "fchDevolucionEquipo",
          "Fecha de devolución",
          "FECHA"
        )
      );
      return List.of(
        f("fechaEntrega", "Fecha de entrega", "FECHA"),
        f("procesador", "Procesador", "TEXTO"),
        f("ram", "Memoria RAM", "TEXTO"),
        f("hd", "Disco", "TEXTO"),
        f("marca", "Marca", "TEXTO"),
        f("accesorios", "Accesorios", "TEXTO")
      );
    }
    return List.of(
      f("usuario", "Usuario / correo", "TEXTO"),
      f("detalle", "Detalle de la solicitud", "TEXTO"),
      f("fecha", "Fecha requerida", "FECHA")
    );
  }

  private static ApprovalEditableFieldDTO f(
    String key,
    String label,
    String type
  ) {
    return new ApprovalEditableFieldDTO(key, label, type);
  }

  public static String update(
    ObjectMapper mapper,
    String type,
    String json,
    String area,
    Map<String, Object> changes
  ) {
    if (
      changes == null || changes.isEmpty()
    ) throw new IllegalArgumentException("No hay cambios que guardar.");
    if ("FMI".equals(area)) {
      Set<String> allowed = new HashSet<>();
      fields(type, area).forEach(f -> allowed.add(f.campo()));
      if (
        !allowed.containsAll(changes.keySet())
      ) throw new IllegalArgumentException("El campo no pertenece a FMI.");
      // El JSON adicional de credenciales no debe afectar la validación del formulario FMI.
      try {
        ObjectNode form = (ObjectNode) mapper.readTree(json);
        JsonNode credentials = form.remove("credenciales");
        ObjectNode updated = (ObjectNode) mapper.readTree(
          ApprovalFormPolicy.apply(
            mapper,
            type,
            mapper.writeValueAsString(form),
            changes
          ).datosFormulario()
        );
        if (credentials != null) updated.set("credenciales", credentials);
        return mapper.writeValueAsString(updated);
      } catch (java.io.IOException e) {
        throw new IllegalArgumentException(
          "El formulario guardado no es válido."
        );
      }
    }
    try {
      ObjectNode form = (ObjectNode) mapper.readTree(json);
      JsonNode dest = source(type, form, area);
      if (
        "CREDENCIALES".equals(area) &&
        !"CREDENCIALES".equals(type) &&
        !dest.isObject()
      ) dest = form.putObject("credenciales");
      if (
        !(dest instanceof ObjectNode target)
      ) throw new IllegalArgumentException(
        "No existe una solicitud para esta área."
      );
      for (var change : changes.entrySet()) {
        var field = fields(type, area)
          .stream()
          .filter(f -> f.campo().equals(change.getKey()))
          .findFirst()
          .orElseThrow(() ->
            new IllegalArgumentException("Campo no editable.")
          );
        if (
          change.getValue() != null && !(change.getValue() instanceof String)
        ) throw new IllegalArgumentException("Valor de campo inválido.");
        String value =
          change.getValue() == null ? "" : change.getValue().toString().trim();
        if (value.length() > 500) throw new IllegalArgumentException(
          "El texto supera los 500 caracteres."
        );
        if ("FECHA".equals(field.tipo()) && !value.isEmpty()) LocalDate.parse(
          value
        );
        if (
          "CESE".equals(type) &&
          "fchDevolucionEquipo".equals(field.campo()) &&
          value.isEmpty()
        ) throw new IllegalArgumentException(
          "Indique la fecha de devolución del equipo."
        );
        target.put(change.getKey(), value);
      }
      return mapper.writeValueAsString(form);
    } catch (java.io.IOException | java.time.DateTimeException e) {
      throw new IllegalArgumentException(
        "Los datos del formulario no son válidos."
      );
    }
  }
}
