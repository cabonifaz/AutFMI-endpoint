package org.app.autfmi.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.report.*;
import org.app.autfmi.model.request.SolicitudSoftwareRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class PDFUtils {

    private final Logger logger = LoggerFactory.getLogger(PDFUtils.class);

    public enum TemplateType {
        SOLICITUD,
        SOLICITUD_EQUIPO,
        FORMULARIO, // PARA INGRESO MOVIMIENTO Y CESE
    }

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emisorCorreo;

    public String loadImage(String pathImage) {
        String base64Image = "";

        Resource resource = new ClassPathResource(pathImage);

        try (InputStream inputStream = resource.getInputStream()) {
            byte[] data = inputStream.readAllBytes();
            base64Image = Base64.getEncoder().encodeToString(data);
        } catch (IOException e) {
            System.out.println("Error al cargar la imagen PDF en la ruta: " + pathImage + " :   " + e.getMessage());
        }

        return base64Image;
    }

    public byte[] crearPDF(String htmlContent, String title) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            htmlContent = htmlContent.replace("{{title}}", title);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(htmlContent, "file:///");
            builder.toStream(baos);
            builder.run();

            return baos.toByteArray();
        } catch (Exception e) {
            System.out.println("Error al generar PDF: " + e.getMessage());
            return null;
        } finally {
            try {
                baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String filePDFToBase64(byte[] byteArchivo) {
        if (byteArchivo == null || byteArchivo.length == 0) {
            throw new IllegalArgumentException("Archivo nulo");
        }

        return Base64.getEncoder().encodeToString(byteArchivo);
    }

    public void enviarCorreoConPDF(List<FileDTO> lstfiles,
            @NonNull String to,
            @NonNull List<String> copyTo,
            @NonNull String subject,
            @NonNull String text)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(emisorCorreo != null ? emisorCorreo : "");

        String dest = to;

        if (dest == null || dest.trim().isBlank()) {
            this.logger.error("Correo cancelado porque no hay un destinatario adecuado");
            return;
        }

        helper.setTo(dest);
        helper.setSubject(subject);
        helper.setText(text);

        if (copyTo != null) {
            // Filtramos correos nulos, vacíos o que sean solo espacios
            // También eliminamos al destinatario principal de la lista de CC
            String[] cleanCc = copyTo.stream()
                    .filter(email -> email != null && !email.trim().isEmpty())
                    .map(String::trim)
                    .filter(email -> !email.equalsIgnoreCase(to.trim()))
                    .toArray(String[]::new);

            this.logger.info("Cleaned CC: {}", cleanCc.length);

            if (cleanCc.length > 0) {
                // setCc solo se ejecuta si hay direcciones válidas
                helper.setCc(cleanCc);
            }
        }

        for (FileDTO objfile : lstfiles) {
            if (objfile.getByteArchivo() != null) {
                ByteArrayDataSource dataSource = new ByteArrayDataSource(objfile.getByteArchivo(), "application/pdf");
                helper.addAttachment(objfile.getNombreArchivo() + ".pdf", dataSource);
            }
        }

        mailSender.send(message);
    }

    public String getHtmlTemplate(TemplateType reportType) {
        String imageB64 = switch (reportType) {
            case SOLICITUD, SOLICITUD_EQUIPO -> loadImage("assets/logo-fractal-2.png");
            case FORMULARIO -> loadImage("assets/logo-fractal.png");
        };

        return switch (reportType) {
            case SOLICITUD -> Constante.FORM_TEMPLATE_SOLICITUD.replace("{{ImgB64}}", imageB64);
            case FORMULARIO -> Constante.FORM_TEMPLATE_EMPLOYEE.replace("{{ImgB64}}", imageB64);
            case SOLICITUD_EQUIPO -> Constante.FORM_TEMPLATE_INS_SOFTWARE.replace("{{ImgB64}}", imageB64);
        };
    }

    /**
     * Reemplaza los valores del reporte de solicitud de equipo en la plantilla HTML
     *
     * @param htmlTemplate La plantilla HTML con marcadores de posición
     * @param report       El objeto SolicitudEquipoReport que contiene los datos a
     *                     insertar
     * @param gs           El objeto GestorDTO que contiene información del gestor
     * @return La plantilla HTML con los valores reemplazados
     */

    public String replaceSolicitudEquipoPDFValues(String htmlTemplate, SolicitudEquipoReport report,
            @NonNull GestorDTO gs) {
        String nombresApellidos = report.getNombreEmpleado() + ' ' + report.getApellidosEmpleado();
        String checkboxCheckedSymbol = "X";

        String tipoPc = report.getIdTipoEquipo() == 1 ? checkboxCheckedSymbol : "";
        String tipoLaptop = report.getIdTipoEquipo() == 2 ? checkboxCheckedSymbol : "";
        String anexoFijo = report.getIdAnexo() == 1 ? checkboxCheckedSymbol : "";
        String anexoSoftphone = report.getIdAnexo() == 2 ? checkboxCheckedSymbol : "";
        String celularSi = report.getCelular() ? checkboxCheckedSymbol : "";
        String celularNo = !report.getCelular() ? checkboxCheckedSymbol : "";
        String internetSi = report.getInternetMovil() ? checkboxCheckedSymbol : "";
        String internetNo = !report.getInternetMovil() ? checkboxCheckedSymbol : "";

        // lista de productos
        List<String> listaProductos = new ArrayList<>();

        if (report.getLstSoftware() != null) {
            for (int i = 0; i < report.getLstSoftware().size(); i++) {
                SolicitudSoftwareRequest requestSoftware = report.getLstSoftware().get(i);

                listaProductos.add(Constante.LIST_ITEM
                        .replace("{{numeroItem}}", String.valueOf(i + 1))
                        .replace("{{producto}}", requestSoftware.getProducto())
                        .replace("{{version}}", requestSoftware.getProdVersion()));
            }
        }

        String htmlFirma;
        if (report.getFirmaGestor() != null && !report.getFirmaGestor().isBlank()) {

            String signatureB64 = FileUtils.cargarArchivoAws(report.getFirmaGestor());
            String fullBase64 = "data:image/png;base64," + signatureB64;

            htmlFirma = "<img src='" + fullBase64
                    + "' style='max-height: 80px; max-width: 250px; display: inline-block;' />";

        } else {
            // Si no hay firma registrada, mostramos el nombre arriba de la línea
            htmlFirma = "<span style='font-size: 11pt; font-weight: bold; line-height: 80px;'>"
                    + report.getNombreApellidoGestor()
                    + "</span>";
        }

        htmlTemplate = htmlTemplate
                .replace("{{title}}", "FT-GS-03 Formulario de Requerimiento de Software y Hardware")
                .replace("{{apellidosNombres}}", nombresApellidos)
                .replace("{{cliente}}", report.getCliente())
                .replace("{{area}}", report.getArea())
                .replace("{{cargo}}", report.getPuesto())
                .replace("{{fechaSolicitud}}", Common.parseDateToFormDate(report.getFechaSolicitud()))
                .replace("{{fechaEntrega}}", Common.parseDateToFormDate(report.getFechaEntrega()))
                .replace("{{symbolPc}}", tipoPc)
                .replace("{{symbolLaptop}}", tipoLaptop)
                .replace("{{procesador}}", report.getProcesador())
                .replace("{{ram}}", report.getRam())
                .replace("{{hd}}", report.getHd())
                .replace("{{marca}}", report.getMarca())
                .replace("{{symbolFijo}}", anexoFijo)
                .replace("{{symbolSoftphone}}", anexoSoftphone)
                .replace("{{symbolCelSi}}", celularSi)
                .replace("{{symbolCelNo}}", celularNo)
                .replace("{{symbolIntSi}}", internetSi)
                .replace("{{symbolIntNo}}", internetNo)
                .replace("{{accesorios}}", report.getAccesorios())
                .replace("{{listaProducto}}", String.join("\n", listaProductos))
                .replace("{{seccionFirma}}", htmlFirma)
                .replace("{{nombreGestor}}", SafeValues.safeString(gs.getFullname()))
                .replace("{{fechaEmision}}", Common.getCurrentDateFormatted());

        return htmlTemplate;
    }
}
