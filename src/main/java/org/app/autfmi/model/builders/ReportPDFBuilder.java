package org.app.autfmi.model.builders;

import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.report.CeseReport;
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

  /*
   * public ReportMovimientoBuilder forMovimiento(MovimientoReport report) {
   * return new ReportMovimientoBuilder(pdfUtils, report);
   * }
   * 
   * public ReportIngresoBuilder forIngreso(IngresoReport report) {
   * return new ReportIngresoBuilder(pdfUtils, report);
   * }
   */

}
