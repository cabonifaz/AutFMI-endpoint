package org.app.autfmi.util;

import jakarta.mail.internet.MimeMessage;
import org.app.autfmi.model.dto.GestorRqDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component

public class MailUtils {
    @Value("${spring.mail.username}")
    private String emisorCorreo;

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendRequirementPostulantMail(List<GestorRqDTO> lstGestores, String asunto, String correoBody) {
        try {
            for (GestorRqDTO gestor : lstGestores) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);

                helper.setFrom(emisorCorreo);
                helper.setTo(gestor.getCorreo());
                helper.setSubject(asunto);
                helper.setText(correoBody, true);

                mailSender.send(message);
            }
        } catch (Exception e) {
            System.err.println("ERROR AL ENVIAR CORREO");
            System.err.println(e.getMessage());
        }

    }
}
