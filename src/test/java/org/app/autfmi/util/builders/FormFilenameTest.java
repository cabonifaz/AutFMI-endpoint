package org.app.autfmi.util.builders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import jakarta.mail.Multipart;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.report.*;
import org.app.autfmi.service.impl.NotificacionCorreoSwitch;
import org.app.autfmi.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class FormFilenameTest {
  private final BaseReportBuilder<Void> builder = new BaseReportBuilder<>(null) {
    public List<FileDTO> build() { return List.of(); }
  };

  @Test void ordersNamesAndPreservesUnicodeSafely() {
    assertEquals("Solicitud de Creación - José Muñoz", builder.buildFilename(
        Normalizer.normalize("José", Normalizer.Form.NFD), "  Muñoz  ", "Solicitud de Creación"));
    assertEquals("Formulario", builder.buildFilename(null, null, "Formulario"));
    assertEquals("Formulario - Núñez", builder.buildFilename(null, "Núñez", "Formulario"));
    assertEquals("Formulario - José Muñoz", builder.buildFilename("José/", "Muñoz:*?", "Formulario"));
  }

  @Test void allSixTemplatesAndRealMailPathPreserveAttachmentNames() throws Exception {
    var resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
    var engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
    var entry = new EntryReport(); entry.setNombres("José"); entry.setApellidos("Muñoz Núñez");
    var movement = new MovementReport(); movement.setNombres("José"); movement.setApellidos("Muñoz Núñez");
    var exit = new CeseReport(); exit.setNombres("José"); exit.setApellidos("Muñoz Núñez");
    var equipment = new SolicitudEquipoReport();
    equipment.setNombreEmpleado("José"); equipment.setApellidosEmpleado("Muñoz Núñez");
    equipment.setIdTipoEquipo(2); equipment.setIdAnexo(1);
    var files = new ArrayList<FileDTO>();
    files.addAll(new ReportIngresoBuilder(engine, entry).withFormulario().withCreateUser().build());
    files.addAll(new ReportMovementBuilder(engine, movement).withFormulario().build());
    files.addAll(new ReportCeseBuilder(engine, exit).withFormularioCese().withDeactivateRequest().build());
    files.addAll(new ReportEquipoBuilder(engine, equipment).withFormulario().build());
    var expected = List.of("FT-GTH-12 Formulario de Ingreso", "FT-GS-01 Solicitud de Creación de Usuarios",
        "FT-GTH-12 Formulario de Movimiento", "FT-GTH-12 Formulario Cese",
        "FT-GS-01 Solicitud de Desactivación de Usuarios",
        "FT-GS-03 Solicitud de Requerimiento de Hardware y Software");
    var captured = new ArrayList<jakarta.mail.internet.MimeMessage>();
    boolean localSmtp = Boolean.getBoolean("obs4434.smtp");
    var sender = new JavaMailSenderImpl() {
      @Override public void send(jakarta.mail.internet.MimeMessage message) {
        captured.add(message);
        if (localSmtp) super.send(message);
      }
    };
    sender.setHost("127.0.0.1"); sender.setPort(14434);
    var pdfUtils = new PDFUtils();
    var mailSwitch = mock(NotificacionCorreoSwitch.class);
    when(mailSwitch.permitido(any())).thenReturn(true);
    ReflectionTestUtils.setField(pdfUtils, "mailSender", sender);
    ReflectionTestUtils.setField(pdfUtils, "emisorCorreo", "obs4434@example.test");
    ReflectionTestUtils.setField(pdfUtils, "correoSwitch", mailSwitch);
    var output = Path.of("target", "obs-4434"); Files.createDirectories(output);
    for (int i = 0; i < files.size(); i++) {
      String name = expected.get(i) + " - José Muñoz Núñez";
      assertEquals(name, files.get(i).getNombreArchivo());
      assertEquals("%PDF", new String(files.get(i).getByteArchivo(), 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
      Files.write(output.resolve(name + ".pdf"), files.get(i).getByteArchivo());
    }
    assertTrue(pdfUtils.enviarCorreoConPDF(EventoNotificacion.INGRESO, files, "talento@example.test",
        List.of(), "OBS-4434: seis formularios con tildes y ñ", "Prueba local sin destinatarios reales."));
    assertEquals(1, captured.size());
    captured.get(0).saveChanges();
    var parts = (Multipart) captured.get(0).getContent();
    assertEquals(7, parts.getCount());
    for (int i = 0; i < files.size(); i++)
      assertEquals(expected.get(i) + " - José Muñoz Núñez.pdf", parts.getBodyPart(i + 1).getFileName());
    try (var stream = Files.newOutputStream(output.resolve("correo.eml"))) { captured.get(0).writeTo(stream); }
  }
}
