package org.app.autfmi.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

class EmployeeRepositoryTest {
  @Test
  void keepsRequestsOnTheirOwnContractAndPreservesUnknownValues() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    doAnswer(invocation -> {
      RowCallbackHandler handler = invocation.getArgument(1);
      // Reverse order to ensure enrichment matches by contract ID, not row order.
      for (int id : new int[] {3, 2, 1}) {
        var row = mock(ResultSet.class);
        when(row.getInt("ID_CONTRATO")).thenReturn(id);
        when(row.getObject("ID_CLIENTE", Integer.class)).thenReturn(id == 3 ? null : 90);
        when(row.getObject("SCTR", Integer.class)).thenReturn(id == 3 ? null : id == 1 ? 1 : 0);
        when(row.getObject("EMO", Integer.class)).thenReturn(id == 3 ? null : id == 1 ? 0 : 1);
        handler.processRow(row);
      }
      return null;
    }).when(jdbc).query(anyString(), any(RowCallbackHandler.class), eq(42));

    try (var calls = mockConstruction(SimpleJdbcCall.class, (call, context) -> {
      when(call.withProcedureName("SP_EMPLEADOS_DETALLES_SEL")).thenReturn(call);
      when(call.execute(any(SqlParameterSource.class))).thenReturn(Map.of(
          "#result-set-1", List.of(Map.of("ID_TIPO_MENSAJE", 2, "MENSAJE", "OK")),
          "#result-set-3", List.of(Map.of("ID_CONTRATO", 1), Map.of("ID_CONTRATO", 2),
              Map.of("ID_CONTRATO", 3), Map.of("ID_CONTRATO", 4))));
    })) {
      var contracts = new EmployeeRepository(jdbc).getEmployeeFullHistory(null, 42).getContracts();
      assertEquals(4, contracts.size());
      assertEquals(1, contracts.get(0).getSctr());
      assertEquals(0, contracts.get(0).getEmo());
      assertEquals(0, contracts.get(1).getSctr());
      assertEquals(1, contracts.get(1).getEmo());
      assertEquals(90, contracts.get(0).getClientId());
      assertNull(contracts.get(2).getSctr());
      assertNull(contracts.get(2).getEmo());
      assertNull(contracts.get(2).getClientId());
      assertNull(contracts.get(3).getSctr());
      assertNull(contracts.get(3).getEmo());
    }
  }
}