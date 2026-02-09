package org.app.autfmi.model.builders;

import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.report.SolicitudEquipoReport;
import org.app.autfmi.util.PDFUtils;
import org.springframework.stereotype.Component;
import org.thymeleaf.spring6.SpringTemplateEngine;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportPDFBuilder {

  private final PDFUtils pdfUtils;
  private final SpringTemplateEngine templateEngine;

  public ReportCeseBuilder forCese(CeseReport report, GestorDTO gs) {
    return new ReportCeseBuilder(this.templateEngine, report, gs);
  }

  public ReportMovementBuilder forMovimiento(MovementReport report, GestorDTO gs) {
    return new ReportMovementBuilder(this.templateEngine, report, gs);
  }

  public ReportIngresoBuilder forIngreso(EntryReport report, GestorDTO gs) {
    return new ReportIngresoBuilder(this.templateEngine, report, gs);
  }

  public ReportEquipoBuilder fEquipoReport(SolicitudEquipoReport report, GestorDTO gs) {
    return new ReportEquipoBuilder(pdfUtils, report, gs);
  }

}
