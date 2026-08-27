package org.app.autfmi.util.builders;

import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

public class ReportIngresoBuilder extends BaseReportBuilder<EntryReport> {

  private SpringTemplateEngine templateEngine;

  protected ReportIngresoBuilder(SpringTemplateEngine templateEngine, EntryReport report) {
    super(report);
    this.templateEngine = templateEngine;
  }

  public ReportIngresoBuilder withFormulario() {

    var context = new Context();

    var logoBase64 = this.imageToBase64("assets/logo-fractal.png");

    // Determinar si es locador
    var esLocador = "Locación de servicios".equalsIgnoreCase(report.getModalidad());
    context.setVariable("locador", esLocador);

    context.setVariable("fractalLogo", logoBase64);

    // Datos colobarador
    context.setVariable("nombreColaborador", report.getNombres() + " " + report.getApellidos());
    context.setVariable("unidad", report.getUnidad());

    // Si el area (Equipo) es Outsourcing, el formulario muestra "Cliente: <cliente>"
    // en lugar de la fila "Equipo".
    boolean esOutsourcing = report.getUnidad() != null
        && Constante.AREA_OUTSOURCING.equalsIgnoreCase(report.getUnidad().trim());
    context.setVariable("esOutsourcing", esOutsourcing);
    context.setVariable("cliente", report.getCliente());

    // Ingreso
    context.setVariable("modalidadIngreso", report.getModalidad());
    context.setVariable("motivoIngreso", report.getMotivo());
    context.setVariable("cargoIngreso", report.getCargo());
    context.setVariable("horarioIngreso", report.getHorario());

    // Declara SUNAT
    if (!esLocador) {
      context.setVariable("declaraSunat", "Sí");
      context.setVariable("sedeDeclarar", report.getSedeDeclararSunat());
    } else {
      context.setVariable("declaraSunat", "No");
      context.setVariable("sedeDeclarar", "");
    }

    // Estructura salarial

    // Sanetizar montos, evitar que se escriban 0.00
    String montoBase = this.sanitizeMoney(report.getMontoBase());
    String montoMovilidad = this.sanitizeMoney(report.getMontoMovilidad());
    String montoTrimestral = this.sanitizeMoney(report.getMontoTrimestral());

    context.setVariable("montoBaseIngreso", montoBase);
    context.setVariable("movilidadIngreso", montoMovilidad);
    context.setVariable("montoTrimestralIngreso", montoTrimestral);
    context.setVariable("montoMensualIngreso", null);

    // Fechas
    context.setVariable("fechaInicioContrato", report.getFechaInicioContrato());
    context.setVariable("fechaFinContrato", report.getFechaFinContrato());
    context.setVariable("proyectoContrato", report.getProyectoServicio());
    context.setVariable("objetoContrato", report.getObjetoContrato());

    // Descagar firma del gestor
    if (report.getFirma() != null && !report.getFirma().isBlank()) {
      var signatureBytes = this.dowloadSignature(report.getFirma());

      // Convertimos los bytes a String Base64 con el prefijo de imagen
      var base64Image = "data:image/png;base64," + signatureBytes;
      context.setVariable("firmaGestor", base64Image);
    } else {
      context.setVariable("firmaGestor", null);
    }

    // Responsable y firma
    context.setVariable("nombreResponsable", report.getFirmante());
    context.setVariable("fechaEmision", Common.getCurrentDateFormatted());

    // Procesar la plantilla (busca 'formulario_movimiento.html' en templates)
    var htmlContent = templateEngine.process("formulario_movimiento", context);

    // Filename
    var filename = this.buildFilename(report.getNombres(), report.getApellidos(),
        "FT-GTH-12 Formulario de Ingreso");

    var pdfBytes = this.renderPdfFromHtml(htmlContent);

    this.files.add(new FileDTO(filename, htmlContent, pdfBytes));
    return this;
  }

  public ReportIngresoBuilder withCreateUser() {

    var context = new Context();
    var logoBase64 = imageToBase64("assets/logo-fractal-2.png");
    context.setVariable("fractalLogo", logoBase64);

    // Datos del solicitante
    context.setVariable("nombresSolicitante", report.getFirmante());
    context.setVariable("areaSolicitante", report.getUnidad());
    context.setVariable("cargoSolicitante", "");
    context.setVariable("fechaSolicitud", report.getFechaHistorial());
    context.setVariable("anexoSolicitud", report.getUnidad());

    // Creación de usuario
    context.setVariable("nombresUsuario", report.getNombres() + " " + report.getApellidos());
    context.setVariable("usernameIngreso", report.getUsernameEmpleado());
    context.setVariable("emailUsuario", report.getEmailEmpleado());
    context.setVariable("areaUsuario", report.getUnidad());

    if (report.getFirma() != null && !report.getFirma().isBlank()) {

      var signatureBase64 = this.dowloadSignature(report.getFirma());
      context.setVariable("firmaGestor",
          "data:image/png;base64," + signatureBase64);
    } else {
      context.setVariable("firmaGestor", null);
    }

    // Responsable y firma
    context.setVariable("nombreResponsable", report.getFirmante());
    context.setVariable("fechaEmision", Common.getCurrentDateFormatted());

    // Procesar plantilla 'solicitud_usuario.html'
    var htmlContent = templateEngine.process("solicitud_usuario", context);

    // Generar PDF en bytes
    var pdfBytes = this.renderPdfFromHtml(htmlContent);

    // Filename
    var filename = this.buildFilename(report.getNombres(), report.getApellidos(),
        "FT-GS-01 Solicitud de Creación de Usuarios");

    this.files.add(new FileDTO(filename, htmlContent, pdfBytes));
    return this;
  }

  @Override
  public List<FileDTO> build() {
    return this.files;
  }
}