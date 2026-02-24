package org.app.autfmi.model.builders;

import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.util.Common;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

public class ReportMovementBuilder extends BaseReportBuilder<MovementReport> {

  private final SpringTemplateEngine templateEngine;

  public ReportMovementBuilder(SpringTemplateEngine templateEngine, MovementReport report, GestorDTO gs) {
    super(null, report, gs);
    this.templateEngine = templateEngine;
  }

  public ReportMovementBuilder withFormulario() {
    var context = new Context();

    // Logo
    var logoBase64 = imageToBase64("assets/logo-fractal.png");
    context.setVariable("fractalLogo", logoBase64);

    // Datos del Colaborador
    context.setVariable("nombreColaborador", report.getNombres() + " " + report.getApellidos());
    context.setVariable("unidad", report.getUnidad());

    // Estructura salarial

    // Sanetizar montos, evitar que se escriban 0.00
    String montoBase = this.sanitizeMoney(report.getMontoBase());
    String montoMovilidad = this.sanitizeMoney(report.getMontoMovilidad());
    String montoTrimestral = this.sanitizeMoney(report.getMontoTrimestral());

    context.setVariable("montoBaseMov", montoBase);
    context.setVariable("montoMovilidadMov", montoMovilidad);
    context.setVariable("montoTrimestralMov", montoTrimestral);
    // context.setVariable("montoBonoMov", montoTrimestral);

    // Fechas y cambios
    context.setVariable("puestoMovimiento", report.getPuesto());
    context.setVariable("areaMovimiento", report.getArea());
    context.setVariable("fechaMovimiento", report.getFechaHistorial());
    context.setVariable("jornadaMovimiento", report.getHorario());

    if (report.getFirma() != null && !report.getFirma().isBlank()) {
      var signatureBytes = this.dowloadSignature(report.getFirma());

      // Convertimos los bytes a String Base64 con el prefijo de imagen
      var base64Image = "data:image/png;base64," + signatureBytes;
      context.setVariable("firmaGestor", base64Image);
    } else {
      context.setVariable("firmaGestor", null);
    }

    // Responsable y Pie de página
    context.setVariable("nombreResponsable", report.getFirmante());
    context.setVariable("fechaEmision", Common.getCurrentDateFormatted());

    // Procesar plantilla
    var htmlContent = templateEngine.process("formulario_movimiento", context);

    // Generar PDF
    var fullName = report.getNombres() + " " + report.getApellidos();
    var fileName = "FT-GT-12-FMI-MOVIMIENTO-" + fullName;
    var pdfBytes = renderPdfFromHtml(htmlContent);

    this.files.add(new FileDTO(fileName, htmlContent, pdfBytes));
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