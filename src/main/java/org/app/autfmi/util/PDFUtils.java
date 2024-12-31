package org.app.autfmi.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;

@Component
public class PDFUtils {

    public enum TemplateType {
        SOLICITUD,
        MOVIMIENTO, // PARA INGRESO MOVIMIENTO Y CESE
    }

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emisorCorreo;

    public String loadLogoPDF(int idLogo) {
        try {
            String rutaLogo = "";
            if (idLogo == 1) {
                rutaLogo = "src/main/resources/assets/logo-fractal.png";
            } else {
                rutaLogo = "src/main/resources/assets/logo-fractal-2.png";
            }

            File file = new File(rutaLogo);

            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] imageBytes = fileInputStream.readAllBytes();

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            fileInputStream.close();

            return  base64Image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static byte[] crearPDF(String htmlContent) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
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

    public void enviarCorreoConPDF(String htmlContent, String to, String subject, String text) throws MessagingException {
        byte[] pdfBytes;
        try {
            pdfBytes = crearPDF(htmlContent);
        } catch (Exception e) {
            throw new MessagingException("Error al generar el PDF", e);
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(emisorCorreo);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);

        ByteArrayDataSource dataSource = new ByteArrayDataSource(pdfBytes, "application/pdf");
        helper.addAttachment("documento.pdf", dataSource);

        mailSender.send(message);
        System.out.println("Correo enviado correctamente");
    }

    public String getHtmlTemplate(TemplateType reportType) {
        loadLogoPDF(1);
        String imageB64 = switch (reportType) {
            case SOLICITUD -> loadLogoPDF(2);
            case MOVIMIENTO -> loadLogoPDF(1);
        };
        return Constante.FORM_TEMPLATE_EMPLOYEE.replace("{{ImgB64}}", imageB64);
    }

    public String replaceEntryRequestValues(String htmlTemplate, EntryReport report) {
        htmlTemplate = htmlTemplate
                //HEADER
                .replace("{{fecha}}", LocalDate.now().toString())
                // DATOS COLABORADOR
                .replace("{{nombres}}", report.getNombres())
                .replace("{{apellidos}}", report.getApellidos())
                .replace("{{unidad}}", report.getUnidad())
                // INGRESO
                .replace("{{modalidad}}", report.getModalidad())
                .replace("{{motivoIngreso}}", report.getMotivo())
                .replace("{{cargo}}", report.getCargo())
                .replace("{{horarioTrabajo}}", report.getHorarioTrabajo())
                .replace("{{montoBaseIn}}", report.getMontoBase().toString())
                .replace("{{montoMovilidadIn}}", report.getMontoMovilidad().toString())
                .replace("{{montoTrimestralIn}}", report.getMontoTrimestral().toString())
                .replace("{{fechaInicioContrato}}", report.getFechaInicioContrato())
                .replace("{{fechaTerminoContrato}}", report.getFechaFinContrato())
                .replace("{{proyectoServicio}}", report.getProyectoServicio())
                .replace("{{objetoContrato}}", report.getObjetoContrato())
                // SUNAT
                .replace("{{declaradoSunat}}", report.getDeclararSunat() == 1 ? "Si" : "No")
                .replace("{{sedeDeclarar}}", report.getSedeDeclararSunat())
                // MOVIMIENTO
                .replace("{{montoBaseMov}}", "Monto")
                .replace("{{montoMovilidadMov}}", "Monto")
                .replace("{{montoTrimestralMov}}", "Monto")
                .replace("{{puesto}}", "Escribir el nuevo puesto")
                .replace("{{area}}", "Escribir la nueva área")
                .replace("{{jornada}}", "Escribir la nueva jornada")
                .replace("{{fechaMovimiento}}", "Escribir la fecha de movimiento")
                // CESE
                .replace("{{motivoCese}}", "Escribir el motivo de cese")
                .replace("{{fechaCese}}", "Escribir el fecha de cese")
                // FOOTER
                .replace("{{firmante}}", report.getFirmante());
        return htmlTemplate;
    }

    public String replaceMovementRequestValues(String htmlTemplate, MovementReport report) {
        htmlTemplate = htmlTemplate
                //HEADER
                .replace("{{fecha}}", LocalDate.now().toString())
                // DATOS COLABORADOR
                .replace("{{nombres}}", report.getNombres())
                .replace("{{apellidos}}", report.getApellidos())
                .replace("{{unidad}}", report.getUnidad())
                // INGRESO
                .replace("{{modalidad}}", "Escriba la modalidad")
                .replace("{{motivoIngreso}}", "Escriba el motivo de ingreso")
                .replace("{{cargo}}", "Escriba el cargo")
                .replace("{{horarioTrabajo}}", "Escriba el horario de trabajo")
                .replace("{{montoBaseIn}}", "Monto")
                .replace("{{montoMovilidadIn}}", "Monto")
                .replace("{{montoTrimestralIn}}", "Monto")
                .replace("{{fechaInicioContrato}}", "Escriba la fecha de inicio de contrato")
                .replace("{{fechaTerminoContrato}}", "Escriba el fecha de termino de contrato")
                .replace("{{proyectoServicio}}", "Escriba el proyecto ó servicio")
                .replace("{{objetoContrato}}", "Escriba el objeto contrato")
                // SUNAT
                .replace("{{declaradoSunat}}", "")
                .replace("{{sedeDeclarar}}", "Escriba la sede")
                // MOVIMIENTO
                .replace("{{montoBaseMov}}", report.getMontoBase().toString())
                .replace("{{montoMovilidadMov}}", report.getMontoMovilidad().toString())
                .replace("{{montoTrimestralMov}}", report.getMontoTrimestral().toString())
                .replace("{{puesto}}", report.getPuesto())
                .replace("{{area}}", report.getArea())
                .replace("{{jornada}}", report.getJornada())
                .replace("{{fechaMovimiento}}", report.getFechaHistorial())
                // CESE
                .replace("{{motivoCese}}", "Escribir el motivo de cese")
                .replace("{{fechaCese}}", "Escribir el fecha de cese")
                // FOOTER
                .replace("{{firmante}}", report.getFirmante());
        return htmlTemplate;
    }

    public String replaceOutRequestValues(String htmlTemplate, CeseReport report) {
        htmlTemplate = htmlTemplate
                //HEADER
                .replace("{{fecha}}", LocalDate.now().toString())
                // DATOS COLABORADOR
                .replace("{{nombres}}", report.getNombres())
                .replace("{{apellidos}}", report.getApellidos())
                .replace("{{unidad}}", report.getUnidad())
                // INGRESO
                .replace("{{modalidad}}", "Escriba la modalidad")
                .replace("{{motivoIngreso}}", "Escriba el motivo de ingreso")
                .replace("{{cargo}}", "Escriba el cargo")
                .replace("{{horarioTrabajo}}", "Escriba el horario de trabajo")
                .replace("{{montoBaseIn}}", "Monto")
                .replace("{{montoMovilidadIn}}", "Monto")
                .replace("{{montoTrimestralIn}}", "Monto")
                .replace("{{fechaInicioContrato}}", "Escriba la fecha de inicio de contrato")
                .replace("{{fechaTerminoContrato}}", "Escriba el fecha de termino de contrato")
                .replace("{{proyectoServicio}}", "Escriba el proyecto ó servicio")
                .replace("{{objetoContrato}}", "Escriba el objeto contrato")
                // SUNAT
                .replace("{{declaradoSunat}}", "")
                .replace("{{sedeDeclarar}}", "Escriba la sede")
                // MOVIMIENTO
                .replace("{{montoBaseMov}}", "Monto")
                .replace("{{montoMovilidadMov}}", "Monto")
                .replace("{{montoTrimestralMov}}", "Monto")
                .replace("{{puesto}}", "Escribir el nuevo puesto")
                .replace("{{area}}", "Escribir la nueva área")
                .replace("{{jornada}}", "Escribir la nueva jornada")
                .replace("{{fechaMovimiento}}", "Escribir la fecha de movimiento")
                // CESE
                .replace("{{motivoCese}}", report.getMotivo())
                .replace("{{fechaCese}}", report.getFechaHistorial())
                // FOOTER
                .replace("{{firmante}}", report.getFirmante());
        return htmlTemplate;
    }
}
