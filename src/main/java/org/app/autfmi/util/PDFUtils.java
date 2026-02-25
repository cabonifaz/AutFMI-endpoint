package org.app.autfmi.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.app.autfmi.model.dto.FileDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

@Component
public class PDFUtils {

    private final Logger logger = LoggerFactory.getLogger(PDFUtils.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emisorCorreo;

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
}
