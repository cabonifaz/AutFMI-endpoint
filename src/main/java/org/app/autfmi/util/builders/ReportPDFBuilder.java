package org.app.autfmi.util.builders;

import java.util.ArrayList;
import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.report.SolicitudEquipoReport;
import org.springframework.stereotype.Component;
import org.thymeleaf.spring6.SpringTemplateEngine;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportPDFBuilder {

  private final SpringTemplateEngine templateEngine;

  public ReportCeseBuilder forCese(CeseReport report) {
    return new ReportCeseBuilder(this.templateEngine, report);
  }

  public ReportMovementBuilder forMovimiento(MovementReport report) {
    return new ReportMovementBuilder(this.templateEngine, report);
  }

  public ReportIngresoBuilder forIngreso(EntryReport report) {
    return new ReportIngresoBuilder(this.templateEngine, report);
  }

  public ReportEquipoBuilder fEquipoReport(SolicitudEquipoReport report) {
    return new ReportEquipoBuilder(templateEngine, report);
  }

  // ---- PDFs completos de cada formulario: los mismos que salen por correo al
  // enviarse (MailService / NotificationService). Se adjuntan también al correo
  // de confirmación para que el gestor revise exactamente lo que se enviará.

  /** Ingreso: Formulario de ingreso + Solicitud de creación de usuario. */
  public List<FileDTO> adjuntosIngreso(EntryReport report) {
    List<FileDTO> files = new ArrayList<>(forIngreso(report).withFormulario().build());
    files.addAll(forIngreso(report).withCreateUser().build());
    return files;
  }

  /** Movimiento: formulario de movimiento. */
  public List<FileDTO> adjuntosMovimiento(MovementReport report) {
    return forMovimiento(report).withFormulario().build();
  }

  /** Cese: formulario de cese + solicitud de desactivación. */
  public List<FileDTO> adjuntosCese(CeseReport report) {
    return forCese(report).withFormularioCese().withDeactivateRequest().build();
  }

  /** Solicitud de equipo: formulario de equipo. */
  public List<FileDTO> adjuntosEquipo(SolicitudEquipoReport report) {
    return fEquipoReport(report).withFormulario().build();
  }

}
