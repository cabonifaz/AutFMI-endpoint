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

        String dest = to != null ? to.trim() : null;

        if (!EmailUtils.isValidEmail(dest)) {
            this.logger.error("Correo cancelado porque no hay un destinatario adecuado ('{}')", to);
            return;
        }

        // Filtramos correos nulos, vacíos, con formato inválido, duplicados
        // y al propio destinatario principal.
        List<String> cleanCc = EmailUtils.sanitizeRecipients(copyTo, dest);
        this.logger.info("CC final ({}): {}", cleanCc.size(), cleanCc);

        try {
            construirYEnviar(dest, cleanCc, subject, text, lstfiles);
            this.logger.info("Correo con PDF enviado a: {} (cc {}: {})", dest, cleanCc.size(), cleanCc);
        } catch (Exception e) {
            // Un CC inválido a nivel SMTP no debe impedir que el destinatario principal lo reciba.
            this.logger.error("Falló el envío con CC ({}); reintentando solo al destinatario principal",
                    e.getMessage());
            construirYEnviar(dest, List.of(), subject, text, lstfiles);
            this.logger.info("Correo con PDF enviado a: {} (sin CC tras reintento)", dest);
        }
    }

    private void construirYEnviar(String to, List<String> cc, String subject, String text, List<FileDTO> lstfiles)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(emisorCorreo != null ? emisorCorreo : "");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);

        if (cc != null && !cc.isEmpty()) {
            helper.setCc(cc.toArray(new String[0]));
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
