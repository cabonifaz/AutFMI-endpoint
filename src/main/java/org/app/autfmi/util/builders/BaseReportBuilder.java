package org.app.autfmi.util.builders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.util.FileUtils;
import org.app.autfmi.util.SafeValues;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

public abstract class BaseReportBuilder<T> {

  protected final T report;
  protected final List<FileDTO> files = new ArrayList<>();

  protected BaseReportBuilder(T report) {
    this.report = report;
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

  protected String dowloadSignature(String signaturePath) {
    return FileUtils.cargarArchivoAws(signaturePath);
  }

  protected String sanitizeFilename(String filename) {
    if (filename == null)
      return "";
    return Normalizer.normalize(filename, Normalizer.Form.NFD)
        .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "") // Elimina tildes
        .replaceAll("[^a-zA-Z0-9\\s\\-_\\.]", ""); // Elimina caracteres especiales
  }

  /**
   * Nombre del PDF: {@code <Nombre del talento> - <Tipo de formulario>}.
   *
   * El nombre de la persona va primero porque estos PDF viajan como adjunto de
   * correo ({@code PDFUtils.construirYEnviar}) y quien los recibe los archiva
   * por talento, no por codigo de formato. El codigo FT se conserva dentro del
   * tipo, que es el que usa gestion documental.
   *
   * <p>
   * <b>Sin fecha, por decision de negocio.</b> Consecuencia asumida: dos
   * formularios del mismo tipo para la misma persona -dos movimientos, o un
   * cese y una recontratacion- pasan a llamarse igual, y desambiguarlos queda
   * del lado de quien los archiva. Si algun dia estorba, el sitio donde anadir
   * la fecha es este y solo este.
   *
   * @param nombres        nombres del talento; admite nulo
   * @param apellidos      apellidos del talento; admite nulo
   * @param tipoFormulario codigo FT + descripcion, tal cual va en el documento
   */
  protected String buildFilename(String nombres, String apellidos, String tipoFormulario) {
    // Nombre y apellidos se unen aqui y no en cada builder porque asi estaba y
    // uno de los seis se olvidaba el espacio: salia "JuanPerez Gomez".
    var nombreTalento = (SafeValues.safeString(nombres) + " " + SafeValues.safeString(apellidos))
        .replaceAll("\\s+", " ")
        .trim();
    var filename = nombreTalento.isEmpty()
        ? tipoFormulario
        : nombreTalento + " - " + tipoFormulario;
    return this.sanitizeFilename(filename);
  }

}
