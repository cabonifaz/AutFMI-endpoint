package org.app.autfmi.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class Common {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emisorCorreo;


    public static BaseRequest createBaseRequest(UserDTO userDTO, String funcionalidades) {
        BaseRequest baseRequest = new BaseRequest();
        if (userDTO != null) {
            if (userDTO.getRoles() != null && !userDTO.getRoles().isEmpty()) {
                List<Integer> roles = userDTO.getRoles();
                Integer idRol = roles.get(0);
                baseRequest.setIdRol(idRol);
            }

            baseRequest.setIdUsuario(userDTO.getIdUsuario());
            baseRequest.setIdEmpresa(userDTO.getIdEmpresa());
            baseRequest.setFuncionalidades(funcionalidades);
            baseRequest.setUsername(userDTO.getUsuario());

            return baseRequest;
        }
        return baseRequest;
    }

    public static LocalDate formatDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (date != null && !date.isEmpty()) {
            return LocalDate.parse(date, formatter);
        }
        return null;
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


    public String reemplazarValores(String html, String nombre, double saldo) {
        html = html.replace("{{nombre}}", nombre);
        html = html.replace("{{saldo}}", String.valueOf(saldo));
        return html;
    }

}
