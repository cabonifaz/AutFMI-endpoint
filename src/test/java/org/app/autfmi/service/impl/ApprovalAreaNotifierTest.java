package org.app.autfmi.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.app.autfmi.model.dto.*;
import org.app.autfmi.model.dto.aprobacion.*;
import org.app.autfmi.repository.*;
import org.app.autfmi.util.*;
import org.junit.jupiter.api.Test;

class ApprovalAreaNotifierTest {

  @Test
  void rejectionOnlyEmailsApplicantWithNoCc() throws Exception {
    var repository = mock(ApprovalRepository.class);
    var areas = mock(ApprovalAreaRepository.class);
    var contacts = mock(ApprovalNotifier.class);
    var mail = mock(MailService.class);
    var switches = mock(NotificacionCorreoSwitch.class);
    var actor = new UserDTO();
    actor.setIdUsuario(7);
    actor.setIdEmpresa(1);
    actor.setUsuario("gestor");
    var applicant = new ApprovalGestorDTO(8, "solicitante", 2);
    var detail = ApprovalDetailDTO.builder()
      .idSolicitud(10)
      .idCliente(1)
      .tipoSolicitud("INGRESO")
      .build();
    when(repository.reserveMail(10, "RECHAZO_FMI", "gestor", 7)).thenReturn(
      true
    );
    when(switches.permitido(EventoNotificacion.APROBACION_GESTORES)).thenReturn(
      true
    );
    when(areas.applicant(10, 1)).thenReturn(applicant);
    when(contacts.correosGestores(List.of(applicant), 1)).thenReturn(
      List.of("solicitante@example.invalid")
    );
    when(
      mail.sendApprovalArea(
        any(),
        eq(detail),
        eq("FMI"),
        eq("RECHAZADA"),
        eq("Motivo"),
        isNull(),
        eq(List.of("solicitante@example.invalid")),
        eq(List.of())
      )
    ).thenReturn(true);
    assertEquals(
      "ENVIADO",
      new ApprovalAreaNotifier(
        repository,
        areas,
        contacts,
        mail,
        switches
      ).send(detail, "FMI", "RECHAZADA", "Motivo", null, actor)
    );
    verify(areas, never()).managers(any(), any());
    verify(mail, never()).approvalRecipients(any());
    verify(mail, never()).approvalSelectionRecipients();
  }

  @Test
  void mailReservationPreventsDuplicateDelivery() {
    var repository = mock(ApprovalRepository.class);
    var areas = mock(ApprovalAreaRepository.class);
    var contacts = mock(ApprovalNotifier.class);
    var mail = mock(MailService.class);
    var switches = mock(NotificacionCorreoSwitch.class);
    var actor = new UserDTO();
    actor.setIdUsuario(7);
    actor.setUsuario("gestor");
    var detail = ApprovalDetailDTO.builder().idSolicitud(10).build();
    assertNull(
      new ApprovalAreaNotifier(
        repository,
        areas,
        contacts,
        mail,
        switches
      ).send(detail, "FMI", "APROBADA", "", null, actor)
    );
    verifyNoInteractions(mail, areas, contacts, switches);
  }

  @Test void approvalIncludesAreaApplicantManagersAndSelection() throws Exception {
    var repository=mock(ApprovalRepository.class);var areas=mock(ApprovalAreaRepository.class);var contacts=mock(ApprovalNotifier.class);var mail=mock(MailService.class);var switches=mock(NotificacionCorreoSwitch.class);
    var actor=new UserDTO();actor.setIdUsuario(7);actor.setIdEmpresa(1);actor.setUsuario("gestor");
    var applicant=new ApprovalGestorDTO(8,"solicitante",2);var manager=new ApprovalGestorDTO(9,"gestor",2);
    var detail=ApprovalDetailDTO.builder().idSolicitud(10).idCliente(1).tipoSolicitud("INGRESO").build();
    when(repository.reserveMail(10,"AREA_CREDENCIALES","gestor",7)).thenReturn(true);when(switches.permitido(EventoNotificacion.INGRESO)).thenReturn(true);
    when(areas.applicant(10,1)).thenReturn(applicant);when(areas.managers(1,1)).thenReturn(List.of(manager));
    when(contacts.correosGestores(List.of(applicant),1)).thenReturn(List.of("solicitante@example.invalid"));
    when(contacts.correosGestores(List.of(manager),1)).thenReturn(List.of("gestor@example.invalid"));
    when(mail.approvalRecipients("CREDENCIALES")).thenReturn(List.of("soporte@example.invalid"));when(mail.approvalSelectionRecipients()).thenReturn(List.of("seleccion@example.invalid"));
    when(mail.sendApprovalArea(any(),eq(detail),eq("CREDENCIALES"),eq("APROBADA"),eq(""),isNull(),eq(List.of("soporte@example.invalid")),eq(List.of("solicitante@example.invalid","gestor@example.invalid","seleccion@example.invalid")))).thenReturn(true);
    assertEquals("ENVIADO",new ApprovalAreaNotifier(repository,areas,contacts,mail,switches).send(detail,"CREDENCIALES","APROBADA","",null,actor));
  }
}
