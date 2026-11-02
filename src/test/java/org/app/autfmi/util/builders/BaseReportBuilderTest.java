package org.app.autfmi.util.builders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;
import javax.imageio.ImageIO;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.report.EntryReport;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class BaseReportBuilderTest {
  private final BaseReportBuilder<Void> builder = new BaseReportBuilder<>(null) {
    public List<FileDTO> build() { return List.of(); }
  };

  @Test
  void preservesLatinCharactersAndHtmlEntitiesInPdf() throws Exception {
    String expected = "á é í ó ú ü ñ Ñ ¿ ¡";
    String html = "<html><body style='font-family: Arial, sans-serif'><p>" + expected
        + "</p><p>" + Normalizer.normalize(expected, Normalizer.Form.NFD)
        + "</p><b>José Muñoz - Gestión de contratación</b>"
        + "<p>&aacute; &ntilde; &Ntilde; &iquest; &iexcl; &amp;</p>"
        + "<i>a&#769; n&#771; N&#771;</i></body></html>";
    try (var pdf = PDDocument.load(builder.renderPdfFromHtml(html))) {
      String text = new PDFTextStripper().getText(pdf);
      assertEquals(2, text.lines().filter(expected::equals).count(), text);
      assertTrue(text.contains("José Muñoz - Gestión de contratación"), text);
      assertTrue(text.contains("á ñ Ñ ¿ ¡ &"), text);
      assertTrue(text.contains("á ñ Ñ"), text);
    }
  }

  @Test
  void preservesAccentsInActualEmailAttachmentTemplates() throws Exception {
    var resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setCharacterEncoding("UTF-8");
    var engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    var report = new EntryReport();
    report.setNombres(Normalizer.normalize("José", Normalizer.Form.NFD));
    report.setApellidos(Normalizer.normalize("Muñoz", Normalizer.Form.NFD));
    report.setFirmante("María Núñez");
    report.setUnidad("Gestión de contratación");
    report.setModalidad("Locación de servicios");
    report.setCargo("Analista de selección");
    report.setObjetoContrato("á é í ó ú ü ñ Ñ ¿ ¡");
    var files = new ReportIngresoBuilder(engine, report).withFormulario().withCreateUser().build();
    assertEquals(2, files.size());
    for (int i = 0; i < files.size(); i++) {
      byte[] bytes = files.get(i).getByteArchivo();
      try (var pdf = PDDocument.load(bytes)) {
        String text = new PDFTextStripper().getText(pdf);
        assertTrue(text.contains("José Muñoz"), text);
        assertTrue(text.contains("María Núñez"), text);
        if (i == 0) assertTrue(text.contains("á é í ó ú ü ñ Ñ ¿ ¡"), text);
        Path output = Path.of("target", "obs-4364");
        Files.createDirectories(output);
        Files.write(output.resolve("formulario-" + i + ".pdf"), bytes);
        ImageIO.write(new PDFRenderer(pdf).renderImageWithDPI(0, 130), "png",
            output.resolve("formulario-" + i + ".png").toFile());
      }
    }
  }
}
