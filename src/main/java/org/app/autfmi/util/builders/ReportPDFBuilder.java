package org.app.autfmi.util.builders;

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

}
