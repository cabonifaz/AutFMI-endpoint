package org.app.autfmi.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.app.autfmi.model.dto.*;
import org.app.autfmi.model.dto.aprobacion.*;
import org.app.autfmi.model.request.*;
import org.app.autfmi.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ApprovalAreaServiceTest {

  final ApprovalService auth = mock(ApprovalService.class);
  final ApprovalWorkflowService workflow = mock(ApprovalWorkflowService.class);
  final ApprovalRepository requests = mock(ApprovalRepository.class);
  final ApprovalAreaRepository areas = mock(ApprovalAreaRepository.class);
  final EmployeeOperationExecutor executor = mock(
    EmployeeOperationExecutor.class
  );
  final EmployeeRepository employees = mock(EmployeeRepository.class);
  final HistoryRepository history = mock(HistoryRepository.class);
  final ApprovalAreaNotifier notifier = mock(ApprovalAreaNotifier.class);
  final ApprovalAreaService service = new ApprovalAreaService(
    auth,
    workflow,
    requests,
    areas,
    executor,
    employees,
    history,
    notifier,
    new ObjectMapper()
  );
  UserDTO user;
  ApprovalDetailDTO detail;

  @BeforeEach
  void setup() {
    user = new UserDTO();
    user.setIdUsuario(7);
    user.setIdEmpresa(1);
    user.setUsuario("gestor");
    user.setIdRoles(List.of(2));
    detail = ApprovalDetailDTO.builder()
      .idSolicitud(10)
      .tipoSolicitud("INGRESO")
      .estado("PENDIENTE")
      .idCliente(1)
      .idTalento(12)
      .datosFormulario("{}")
      .build();
    when(auth.authenticate("token")).thenReturn(user);
    when(requests.getRequest(eq(10), eq(user), anyString())).thenReturn(
      ApprovalResult.of(ApprovalOutcome.OK, 2, "OK", detail)
    );
    when(workflow.getFlow("token", 10)).thenReturn(
      ApprovalResult.of(
        ApprovalOutcome.OK,
        2,
        "OK",
        new ApprovalFlowDTO(
          "INGRESO",
          "PENDIENTE",
          true,
          true,
          9L,
          List.of(),
          List.of()
        )
      )
    );
    when(areas.states(10)).thenReturn(Map.of());
    when(areas.reviewed(10, user, 9L)).thenReturn(
      List.of("FMI", "EQUIPO", "CREDENCIALES")
    );
    when(areas.finish(10, user)).thenReturn(
      new ApprovalDecisionDTO(10, "PENDIENTE", 10L)
    );
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void clear() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void onlyCredentialsDoesNotExecuteFmiOrEquipment() {
    var result = service.decide(
      "token",
      10,
      new ApprovalAreaDecisionRequest(
        9L,
        List.of("CREDENCIALES"),
        "APROBAR",
        null,
        false
      )
    );
    assertEquals("PENDIENTE", result.estado());
    verifyNoInteractions(executor, employees, history, notifier);
    verify(areas).resolve(10, "CREDENCIALES", "APROBADA", user, "");
    TransactionSynchronizationManager.getSynchronizations().forEach(s ->
      s.afterCommit()
    );
    verify(notifier).notifyArea(
      detail,
      "CREDENCIALES",
      "APROBADA",
      "",
      null,
      user
    );
  }

  @Test
  void rejectingOneAreaNeverExecutesOperations() {
    service.decide(
      "token",
      10,
      new ApprovalAreaDecisionRequest(
        9L,
        List.of("FMI"),
        "RECHAZAR",
        "Dato incorrecto",
        false
      )
    );
    verify(areas).resolve(10, "FMI", "RECHAZADA", user, "Dato incorrecto");
    verifyNoInteractions(executor, employees, history);
  }

  @Test
  void staleVersionCannotDecide() {
    var error = assertThrows(ApprovalAreaService.DecisionException.class, () ->
      service.decide(
        "token",
        10,
        new ApprovalAreaDecisionRequest(
          8L,
          List.of("FMI"),
          "APROBAR",
          null,
          false
        )
      )
    );
    assertEquals(ApprovalOutcome.CONFLICTO, error.outcome);
    verify(areas, never()).resolve(any(), any(), any(), any(), any());
  }

  @Test
  void allRequiresOnlyApplicableAreas() {
    detail.setDatosFormulario("{\"idTalento\":12}");
    when(areas.reviewed(10, user, 9L)).thenReturn(
      List.of("FMI", "CREDENCIALES")
    );
    when(executor.executeEntry(any(), any())).thenReturn(
      new EmployeeOperationExecutor.Execution(
        new org.app.autfmi.model.response.BaseResponse(2, "OK"),
        null
      )
    );
    assertDoesNotThrow(() ->
      service.decide(
        "token",
        10,
        new ApprovalAreaDecisionRequest(
          9L,
          List.of("FMI", "CREDENCIALES"),
          "APROBAR",
          null,
          true
        )
      )
    );
    verify(areas, never()).resolve(eq(10), eq("EQUIPO"), any(), any(), any());
  }

  @Test
  void resolvedAreaCannotBeDecidedTwice() {
    when(areas.states(10)).thenReturn(
      Map.of("CREDENCIALES", Map.of("ESTADO", "APROBADA"))
    );
    assertThrows(ApprovalAreaService.DecisionException.class, () ->
      service.decide(
        "token",
        10,
        new ApprovalAreaDecisionRequest(
          9L,
          List.of("CREDENCIALES"),
          "APROBAR",
          null,
          false
        )
      )
    );
    verify(areas, never()).resolve(any(), any(), any(), any(), any());
  }

  @Test
  void rejectingWithoutReasonIsRejectedBeforeAnyDatabaseLock() {
    assertThrows(ApprovalAreaService.DecisionException.class, () ->
      service.decide(
        "token",
        10,
        new ApprovalAreaDecisionRequest(
          9L,
          List.of("FMI"),
          "RECHAZAR",
          " ",
          false
        )
      )
    );
    verify(areas, never()).lock(any());
  }

  @Test
  void approvingTwoAreasLeavesEquipmentUnchanged() throws Exception {
    detail.setDatosFormulario("{\"idTalento\":12}");
    when(executor.executeEntry(any(), any())).thenReturn(
      new EmployeeOperationExecutor.Execution(
        new org.app.autfmi.model.response.BaseResponse(2, "OK"),
        null
      )
    );
    service.decide(
      "token",
      10,
      new ApprovalAreaDecisionRequest(
        9L,
        List.of("FMI", "CREDENCIALES"),
        "APROBAR",
        null,
        false
      )
    );
    verify(areas).resolve(10, "FMI", "APROBADA", user, "");
    verify(areas).resolve(10, "CREDENCIALES", "APROBADA", user, "");
    verify(areas, never()).resolve(eq(10), eq("EQUIPO"), any(), any(), any());
    verify(employees, never()).insertSolicitudEquipo(any(), any());
  }

  @Test
  void failedOperationDoesNotConfirmDecision() {
    detail.setDatosFormulario("{\"idTalento\":12}");
    when(executor.executeEntry(any(), any())).thenReturn(
      new EmployeeOperationExecutor.Execution(
        new org.app.autfmi.model.response.BaseResponse(
          1,
          "Operación rechazada"
        ),
        null
      )
    );
    var error = assertThrows(ApprovalAreaService.DecisionException.class, () ->
      service.decide(
        "token",
        10,
        new ApprovalAreaDecisionRequest(
          9L,
          List.of("FMI"),
          "APROBAR",
          null,
          false
        )
      )
    );
    assertEquals(ApprovalOutcome.NO_PROCESABLE, error.outcome);
    verify(areas, never()).resolve(any(), any(), any(), any(), any());
    verify(areas, never()).finish(any(), any());
    assertTrue(
      TransactionSynchronizationManager.getSynchronizations().isEmpty()
    );
  }
}
