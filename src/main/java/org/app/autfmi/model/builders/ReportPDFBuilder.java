package org.app.autfmi.model.builders;

import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.util.PDFUtils;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportPDFBuilder {
  private final PDFUtils pdfUtils;

  public ReportCeseBuilder forCese(CeseReport report, GestorDTO gs) {
    return new ReportCeseBuilder(pdfUtils, report, gs);
  }

  public ReportMovementBuilder forMovimiento(MovementReport report, GestorDTO gs) {
    return new ReportMovementBuilder(pdfUtils, report, gs);
  }

  /*
   * public ReportIngresoBuilder forIngreso(IngresoReport report) {
   * return new ReportIngresoBuilder(pdfUtils, report);
   * }
   */

}
