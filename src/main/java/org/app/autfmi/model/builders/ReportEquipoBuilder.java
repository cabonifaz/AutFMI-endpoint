package org.app.autfmi.model.builders;

import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.report.SolicitudEquipoReport;
import org.app.autfmi.util.PDFUtils;

public class ReportEquipoBuilder extends BaseReportBuilder<SolicitudEquipoReport> {

  protected ReportEquipoBuilder(PDFUtils pdfUtils, SolicitudEquipoReport report, GestorDTO gs) {
    super(pdfUtils, report, gs);
  }

  public ReportEquipoBuilder withFormulario() {

    String employeeName = report.getNombreEmpleado() + " " + report.getApellidosEmpleado();
    String template = this.pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD_EQUIPO);
    String filled = pdfUtils.replaceSolicitudEquipoPDFValues(template, report, gs);
    String filename = "FT-GS-03-FMI-" + employeeName;
    this.files.add(new FileDTO(filename, filled, null));
    return this;
  }

  @Override
  public List<FileDTO> build() {
    return this.files;
  }
}
