package org.app.autfmi.model.builders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.util.PDFUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

public abstract class BaseReportBuilder<T> {

  protected final PDFUtils pdfUtils;
  protected final T report;
  @NonNull
  protected final GestorDTO gs;
  protected final List<FileDTO> files = new ArrayList<>();

  protected BaseReportBuilder(PDFUtils pdfUtils, T report, GestorDTO gs) {
    if (gs == null)
      throw new IllegalArgumentException("GestorDTO cannot be null");

    this.pdfUtils = pdfUtils;
    this.report = report;
    this.gs = gs;
  }

  public abstract List<FileDTO> build();

  /**
   * Render an HTML template to an array of bytes
   * 
   * @param html
   * @return
   */
  public byte[] renderPdfFromHtml(String html) {
    try (var os = new ByteArrayOutputStream()) {

      var document = Jsoup.parse(html);
      document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

      var xhtml = document.html();
      var builder = new PdfRendererBuilder();

      builder.useFastMode();
      builder.withHtmlContent(xhtml, null);
      builder.toStream(os);
      builder.run();

      return os.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException("Error al renderizar PDF desde HTML", e);
    }
  }

  public String imageToBase64(@NonNull String path) {
    try {
      var imgFile = new ClassPathResource(path);
      var bytes = StreamUtils.copyToByteArray(imgFile.getInputStream());
      var mimeType = "image/png";
      return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    } catch (IOException e) {
      // Log error o devuelve un string vacío/logo por defecto
      System.err.println("No se pudo cargar el logo: " + e.getMessage());
      return "";
    }
  }

  public String sanitizeMoney(String value) {
    if (value == null)
      return null;

    // REGEX EXPLICACIÓN:
    // ^ -> Inicio de línea
    // [\D]* -> Cualquier cantidad de caracteres NO dígitos (monedas, espacios,
    // letras)
    // 0 -> Un cero obligatorio
    // (?: -> Grupo de no captura para decimales
    // [.,] -> Punto o coma decimal
    // 0+ -> Uno o más ceros
    // )? -> El grupo decimal es opcional
    // $ -> Fin de línea
    if (value.matches("^[\\D]*0(?:[.,]0+)?$")) {
      return null;
    }
    return value;
  }

}
