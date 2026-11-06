package org.app.autfmi.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.*;
import org.app.autfmi.service.IMailService;
import org.app.autfmi.util.*;
import org.app.autfmi.util.builders.ReportPDFBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeDirectEntryTest {
  @Mock IMailService mail;
  @Mock EmployeeRepository employees;
  @Mock HistoryRepository history;
  @Mock PDFUtils pdf;
  @Mock JwtHelper jwt;
  @Mock ReportPDFBuilder reports;
  @Mock SelectionEntryExecutor selection;
  @Mock NotificationService notifications;
  @InjectMocks EmployeeService service;
  EmployeeEntryRequest request;

  @BeforeEach void setup() {
    var user = new UserDTO();
    user.setIdUsuario(1); user.setIdEmpresa(1); user.setUsuario("test"); user.setIdRoles(List.of(1));
    when(jwt.decodeToken("token")).thenReturn(user);
    request = new EmployeeEntryRequest();
    request.setIdTalento(42); request.setIdRequerimiento(10); request.setIdCliente(20);
  }

  @Test void entryWithRequirementRegistersImmediatelyAndSendsMail() throws Exception {
    var report = new EntryReport(); report.setResponse(new BaseResponse(2, "OK"));
    when(history.registerEntry(any(), same(request))).thenReturn(report);
    assertSame(report.getResponse(), service.saveEmployeeEntry("token", request));
    verify(history).registerEntry(any(), same(request));
    verify(mail).sendEntryReportNotification(report, 42);
    verifyNoInteractions(selection);
  }

  @Test void rejectedEntryDoesNotSendMail() throws Exception {
    var report = new EntryReport(); report.setResponse(new BaseResponse(1, "Rejected"));
    when(history.registerEntry(any(), same(request))).thenReturn(report);
    assertEquals(1, service.saveEmployeeEntry("token", request).getIdTipoMensaje());
    verifyNoInteractions(mail, selection);
  }

  @Test void selectionUsesTransactionalExecutorWithoutApproval() throws Exception {
    request.setSeleccion(mock(RequirementTalentRequestDTO.class));
    var response = new BaseResponse(1, "Selection changed");
    when(selection.execute(any(), same(request))).thenReturn(new SelectionEntryExecutor.Execution(response, null));
    assertSame(response, service.saveEmployeeEntry("token", request));
    verifyNoInteractions(history, mail, notifications);
  }
}

