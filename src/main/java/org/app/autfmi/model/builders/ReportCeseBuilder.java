package org.app.autfmi.model.builders;

import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.SolicitudData;
import org.app.autfmi.util.PDFUtils;

public class ReportCeseBuilder extends BaseReportBuilder<CeseReport> {

  public ReportCeseBuilder(PDFUtils pdfUtils, CeseReport report, GestorDTO gs) {
    super(pdfUtils, report, gs);
  }

  public ReportCeseBuilder withFormulario() {
    String fullName = report.getNombres() + " " + report.getApellidos();
    String fileName = "FT-GT-12-FMI-CESE-" + fullName;
    String fileTemplate = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO);
    String filled = pdfUtils.replaceOutRequestValues(fileTemplate, report, gs);

    files.add(new FileDTO(fileName, filled, null));
    return this;
  }

  public ReportCeseBuilder withDeactivateRequest() {

    SolicitudData data = new SolicitudData();
    data.setNombres(report.getNombres());
    data.setApellidos(report.getApellidos());
    data.setArea(report.getUnidad());
    data.setFechaSolicitud(report.getFechaHistorial());
    data.setNombresCese(report.getNombres());
    data.setApellidosCese(report.getApellidos());
    data.setUsuarioCese(report.getUsernameEmpleado());
    data.setCorreoCese(report.getEmailEmpleado());
    data.setMotivoCese(report.getMotivo());
    data.setFirmante(report.getFirmante());

    String fullName = report.getNombres() + " " + report.getApellidos();
    String fileName = "FT-GS-01-FMI-DESACTIVAR-USUARIO-" + fullName;
    String fileTemplate = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD);
    String templateWithValues = pdfUtils.replaceSolicitudPDFValues(fileTemplate, data, gs);

    files.add(new FileDTO(fileName, templateWithValues, null));
    return this;
  }

  @Override
  public List<FileDTO> build() {
    return this.files;
  }
}
