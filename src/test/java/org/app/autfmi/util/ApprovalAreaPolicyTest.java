package org.app.autfmi.util;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApprovalAreaPolicyTest {

  final ObjectMapper mapper = new ObjectMapper();

  @Test
  void movementHasOnlyFmi() throws Exception {
    var form = mapper.readTree("{}");
    assertTrue(ApprovalAreaPolicy.applies("MOVIMIENTO", form, "FMI"));
    assertFalse(ApprovalAreaPolicy.applies("MOVIMIENTO", form, "EQUIPO"));
    assertFalse(ApprovalAreaPolicy.applies("MOVIMIENTO", form, "CREDENCIALES"));
  }

  @Test
  void entryOnlyRequestsEquipmentWhenThereIsARequest() throws Exception {
    assertFalse(
      ApprovalAreaPolicy.applies("INGRESO", mapper.readTree("{}"), "EQUIPO")
    );
    assertTrue(
      ApprovalAreaPolicy.applies(
        "INGRESO",
        mapper.readTree(
          "{\"seleccion\":{\"solicitudEquipo\":{\"marca\":\"Original\"}}}"
        ),
        "EQUIPO"
      )
    );
    assertTrue(
      ApprovalAreaPolicy.applies(
        "INGRESO",
        mapper.readTree("{}"),
        "CREDENCIALES"
      )
    );
  }

  @Test
  void terminationHasReturnAndCredentialDeactivation() throws Exception {
    var form = mapper.readTree("{\"fchDevolucionEquipo\":\"2026-11-02\"}");
    for (String area : ApprovalAreaPolicy.AREAS)
      assertTrue(ApprovalAreaPolicy.applies("CESE", form, area));
    assertFalse(
      ApprovalAreaPolicy.applies("CESE", mapper.readTree("{}"), "EQUIPO")
    );
  }

  @Test
  void updatingEquipmentPreservesFmiAndIds() throws Exception {
    String json =
      "{\"idTalento\":12,\"cargo\":\"Analista\",\"seleccion\":{\"solicitudEquipo\":{\"marca\":\"Original\"}}}";
    var result = mapper.readTree(
      ApprovalAreaPolicy.update(
        mapper,
        "INGRESO",
        json,
        "EQUIPO",
        Map.of("marca", "Nueva")
      )
    );
    assertEquals(12, result.path("idTalento").asInt());
    assertEquals("Analista", result.path("cargo").asText());
    assertEquals(
      "Nueva",
      result.path("seleccion").path("solicitudEquipo").path("marca").asText()
    );
    assertThrows(IllegalArgumentException.class, () ->
      ApprovalAreaPolicy.update(
        mapper,
        "INGRESO",
        json,
        "EQUIPO",
        Map.of("idTalento", 30)
      )
    );
  }

  @Test
  void invalidDatesAndClearingReturnAreRejected() {
    assertThrows(IllegalArgumentException.class, () ->
      ApprovalAreaPolicy.update(
        mapper,
        "CESE",
        "{\"fchDevolucionEquipo\":\"2026-11-02\"}",
        "EQUIPO",
        Map.of("fchDevolucionEquipo", "")
      )
    );
    assertThrows(IllegalArgumentException.class, () ->
      ApprovalAreaPolicy.update(
        mapper,
        "CREDENCIALES",
        "{}",
        "CREDENCIALES",
        Map.of("fecha", "2026-02-30")
      )
    );
  }
}
