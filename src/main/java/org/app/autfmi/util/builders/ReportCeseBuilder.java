package org.app.autfmi.util.builders;

import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.util.Common;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

public class ReportCeseBuilder extends BaseReportBuilder<CeseReport> {

  private final SpringTemplateEngine templateEngine;

  public ReportCeseBuilder(SpringTemplateEngine templateEngine, CeseReport report) {
    super(report);
    this.templateEngine = templateEngine;
  }

  public ReportCeseBuilder withFormularioCese() {
    var context = new Context();

    // Cargar Logo
    var logoBase64 = this.imageToBase64("assets/logo-fractal.png");
    context.setVariable("fractalLogo", logoBase64);

    // Datos Generales (Datos del Colaborador)
    context.setVariable("nombreColaborador", report.getNombres() + " " + report.getApellidos());
    context.setVariable("unidad", report.getUnidad());

    // Sección CESE (Mapeo de datos específicos)
    context.setVariable("motivoCese", report.getMotivo());
    context.setVariable("fechaCese", report.getFechaHistorial());
    context.setVariable("fechaDevolucionEquipo", report.getFchDevolucionEquipo());

    // Firma y Pie de Página
    if (report.getFirma() != null && !report.getFirma().isEmpty()) {
      var signatureBytes = this.dowloadSignature(report.getFirma());
      var base64Image = "data:image/png;base64," + signatureBytes;
      context.setVariable("firmaGestor", base64Image);
    } else {
      context.setVariable("firmaGestor", null);
    }

    context.setVariable("nombreResponsable", report.getFirmante());
    context.setVariable("fechaEmision", Common.getCurrentDateFormatted());

    // Procesar Plantilla
    var htmlContent = templateEngine.process("formulario_movimiento", context);

    // Generar PDF
    var fullName = report.getNombres() + " " + report.getApellidos();
    var fileName = "FT-GT-12-FMI-CESE-" + fullName;
    var pdfBytes = this.renderPdfFromHtml(htmlContent);

    fileName = this.sanitizeFilename(fileName);

    this.files.add(new FileDTO(fileName, htmlContent, pdfBytes));
    return this;
  }

  public ReportCeseBuilder withDeactivateRequest() {
    var context = new Context();

    // 1. Cargar Logo (Usualmente logo-fractal-2.png para solicitudes internas)
    var logoBase64 = this.imageToBase64("assets/logo-fractal-2.png");
    context.setVariable("fractalLogo", logoBase64);

    // 2. Datos del Solicitante (El gestor o firmante)
    context.setVariable("nombresSolicitante", report.getFirmante());
    context.setVariable("areaSolicitante", report.getUnidad());
    context.setVariable("cargoSolicitante", "");
    context.setVariable("fechaSolicitud", report.getFechaHistorial());

    // 3. Sección DESACTIVACIÓN DE USUARIOS
    var nombreCompleto = report.getNombres() + " " + report.getApellidos();
    context.setVariable("nombresDesactivacion", nombreCompleto);
    context.setVariable("usuarioDesactivacion", report.getUsernameEmpleado());
    context.setVariable("correoDesactivacion", report.getEmailEmpleado());
    context.setVariable("motivoDesactivacion", report.getMotivo());

    if (report.getFirma() != null && !report.getFirma().isBlank()) {

      var signatureBase64 = this.dowloadSignature(report.getFirma());
      context.setVariable("firmaGestor",
          "data:image/png;base64," + signatureBase64);
    } else {
      context.setVariable("firmaGestor", null);
    }

    // 4. Firma y Pie de Página
    context.setVariable("nombreResponsable", report.getFirmante());
    context.setVariable("fechaEmision", Common.getCurrentDateFormatted());

    // 5. Procesar Plantilla
    var htmlContent = templateEngine.process("solicitud_usuario", context);

    // 6. Generar PDF
    var fullName = report.getNombres() + " " + report.getApellidos();
    var fileName = "FT-GS-01-FMI-DESACTIVAR-USUARIO-" + fullName;
    var pdfBytes = this.renderPdfFromHtml(htmlContent);

    this.files.add(new FileDTO(fileName, htmlContent, pdfBytes));
    return this;
  }

  @Override
  public List<FileDTO> build() {
    return this.files;
  }
}