package org.app.autfmi.model.builders;

import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.util.PDFUtils;

public class ReportMovementBuilder extends BaseReportBuilder<MovementReport> {

  public ReportMovementBuilder(PDFUtils pdfUtils, MovementReport report, GestorDTO gs) {
    super(pdfUtils, report, gs);
  }

  public ReportMovementBuilder withFormulario() {
    String fullName = report.getNombres() + " " + report.getApellidos();
    String fileName = "FT-GT-12-FMI-MOVIMIENTO-" + fullName;
    String fileTemplate = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO);
    String filled = pdfUtils.replaceMovementRequestValues(fileTemplate, report);

    byte[] fileBytes = pdfUtils.crearPDF(filled, fileName);

    files.add(new FileDTO(fileName, filled, fileBytes));
    return this;
  }

  public ReportMovementBuilder withModifyRequest() {
    throw new UnsupportedOperationException("Modify Request not implemented yet");
  }

  @Override
  public List<FileDTO> build() {
    return this.files;
  }
}
