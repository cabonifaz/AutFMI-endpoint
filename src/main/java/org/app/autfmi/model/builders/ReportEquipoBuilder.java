package org.app.autfmi.model.builders;

import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.report.SolicitudEquipoReport;
import org.app.autfmi.util.Common;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

public class ReportEquipoBuilder extends BaseReportBuilder<SolicitudEquipoReport> {

  private SpringTemplateEngine templateEngine;

  protected ReportEquipoBuilder(SpringTemplateEngine templateEngine, SolicitudEquipoReport report, GestorDTO gs) {
    super(null, report, gs);
    this.templateEngine = templateEngine;
  }

  public ReportEquipoBuilder withFormulario() {
    var context = new Context();

    // Logo
    context.setVariable("fractalLogo", this.imageToBase64("assets/logo-fractal-2.png"));

    // Datos del colaborador
    String fullname = report.getNombreEmpleado() + " " + report.getApellidosEmpleado();

    context.setVariable("apellidosNombres", fullname);
    context.setVariable("cliente", report.getCliente());
    context.setVariable("area", report.getArea());
    context.setVariable("cargo", report.getPuesto());
    context.setVariable("dni", report.getDniTalento());
    context.setVariable("telefono", report.getCelularTalento());

    // Fechas
    context.setVariable("fechaSolicitud", Common.parseDateToFormDate(report.getFechaSolicitud()));
    context.setVariable("fechaEntrega", Common.parseDateToFormDate(report.getFechaEntrega()));

    // Hardware – tipo (booleanos, el template decide si pone X o no)
    context.setVariable("tipoPC", report.getIdTipoEquipo() == 1);
    context.setVariable("tipoLaptop", report.getIdTipoEquipo() == 2);
    context.setVariable("procesador", report.getProcesador());
    context.setVariable("ram", report.getRam());
    context.setVariable("hd", report.getHd());
    context.setVariable("marca", report.getMarca());

    // Comunicaciones
    context.setVariable("anexoFijo", report.getIdAnexo() == 1);
    context.setVariable("anexoSoftphone", report.getIdAnexo() == 2);
    context.setVariable("celularSi", Boolean.TRUE.equals(report.getCelular()));
    context.setVariable("celularNo", !Boolean.TRUE.equals(report.getCelular()));
    context.setVariable("internetMovilSi", Boolean.TRUE.equals(report.getInternetMovil()));
    context.setVariable("internetMovilNo", !Boolean.TRUE.equals(report.getInternetMovil()));

    // Accesorios
    context.setVariable("accesorios", report.getAccesorios());

    // Lista de software
    var software = report.getLstSoftware() != null ? report.getLstSoftware() : List.of();
    context.setVariable("softwareList", software);

    // Firma
    if (report.getFirmaGestor() != null && !report.getFirmaGestor().isBlank()) {
      String signatureB64 = this.dowloadSignature(report.getFirmaGestor());
      context.setVariable("firmaGestor", "data:image/png;base64," + signatureB64);
    } else {
      context.setVariable("firmaGestor", null);
    }

    context.setVariable("nombreGestor", report.getNombreApellidoGestor());
    context.setVariable("fechaEmision", Common.getCurrentDateFormatted());

    // Procesar template y generar PDF
    var htmlContent = templateEngine.process("formulario_sol_equipo", context);

    var year = Common.getCurrentYear();
    var monthName = Common.getMonthText();
    var formattedName = fullname.replaceAll("\\s+", "_");

    var filename = new StringBuilder()
        .append("FT-GS-03 Solicitud de Requerimiento de Hardware y Software")
        .append("_")
        .append(year)
        .append("_")
        .append(monthName)
        .append("_")
        .append(formattedName)
        .toString();

    var pdfBytes = this.renderPdfFromHtml(htmlContent);

    this.files.add(new FileDTO(this.sanitizeFilename(filename), htmlContent, pdfBytes));
    return this;
  }

  @Override
  public List<FileDTO> build() {
    return this.files;
  }
}
