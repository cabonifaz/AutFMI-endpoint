package org.app.autfmi.model.builders;

import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.SolicitudData;
import org.app.autfmi.util.PDFUtils;

public class ReportIngresoBuilder extends BaseReportBuilder<EntryReport> {

  protected ReportIngresoBuilder(PDFUtils pdfUtils, EntryReport report, GestorDTO gs) {
    super(pdfUtils, report, gs);
  }

  public ReportIngresoBuilder withFormulario() {

    String template = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO);
    String fullname = report.getNombres() + " " + report.getApellidos();
    String filename = "FT-GT-12-FMI-Formulario de Ingreso " + fullname;

    String filled = pdfUtils.replaceEntryRequestValues(template, report, gs);
    byte[] fileBytes = pdfUtils.crearPDF(filled, filename);

    this.files.add(new FileDTO(filename, filled, fileBytes));
    return this;
  }

  public ReportIngresoBuilder withUsuarioInfo() {
    String filename = "FT-GS-01 Solicitud de Creación de Usuario";

    SolicitudData data = new SolicitudData();
    data.setNombres(report.getNombres());
    data.setApellidos(report.getApellidos());
    data.setArea(report.getUnidad());
    data.setFechaSolicitud(report.getFechaHistorial());
    data.setNombresCreacion(report.getNombres());
    data.setApellidosCreacion(report.getApellidos());
    data.setNombreUsuarioCreacion(report.getUsernameEmpleado());
    data.setCorreoCreacion(report.getEmailEmpleado());
    data.setAreaCreacion(report.getUnidad());
    data.setFirmante(report.getFirmante());

    String template = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD);
    String filled = pdfUtils.replaceSolicitudPDFValues(
        template,
        data, gs);

    byte[] fileBytes = pdfUtils.crearPDF(filled, filename);
    this.files.add(new FileDTO(filename, filled, fileBytes));

    return this;
  }

  @Override
  public List<FileDTO> build() {
    return this.files;
  }

}
