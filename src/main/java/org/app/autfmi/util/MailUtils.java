package org.app.autfmi.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.app.autfmi.model.dto.GestorRqDTO;
import org.app.autfmi.model.dto.PostulantDTO;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component

public class MailUtils {
    @Value("${spring.mail.username}")
    private String emisorCorreo;

    @Autowired
    private JavaMailSender mailSender;

    // Inyecta el motor de plantillas de Thymeleaf
    @Autowired
    private TemplateEngine templateEngine;

    private static final Logger logger = LoggerFactory.getLogger(MailUtils.class);

    @Async
    public void sendRequirementPostulantMail(GestorRqDTO gestor, String asunto, List<PostulantDTO> lstPostulantes,
            List<String> lstEmails) {
        try {
            // lista de talentos al RQ
            List<String> listaTalentosRQ = new ArrayList<>();
            int indice = 1;

            for (PostulantDTO postulante : lstPostulantes) {
                listaTalentosRQ.add(Constante.LIST_TALENT_ROW
                        .replace("{{numFila}}", indice + "")
                        .replace("{{nombres}}", postulante.getNombres())
                        .replace("{{apellidos}}", postulante.getApellidos())
                        .replace("{{docIdentidad}}", postulante.getDni())
                        .replace("{{numCelular}}", postulante.getCelular())
                        .replace("{{correo}}", postulante.getEmail())
                        .replace("{{fchInicioLabores}}", postulante.getFechaInicioLabores())
                        .replace("{{tiempoContrato}}", postulante.getTiempoContrato())
                        .replace("{{cargo}}", postulante.getCargo())
                        .replace("{{remuneracion}}", postulante.getRemuneracion().toString())
                        .replace("{{modalidad}}", postulante.getModalidad())
                        .replace("{{tieneEquipo}}", postulante.getTieneEquipo()));
                indice++;
            }

            String mensajeCorreo = replaceDataToHtmlBody(Constante.CUERPO_CORREO, gestor, listaTalentosRQ);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(emisorCorreo);
            helper.setTo(gestor.getCorreo());

            if (lstEmails != null && !lstEmails.isEmpty()) {
                helper.setCc(lstEmails.toArray(new String[0]));
            }

            helper.setSubject(asunto + " | " + gestor.getCodigoRQ() + " | " + gestor.getCliente());
            helper.setText(mensajeCorreo, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("ERROR AL ENVIAR CORREO");
            System.err.println(e.getMessage());
        }
    }

    private static String replaceDataToHtmlBody(String cuerpoCorreo, GestorRqDTO gestor, List<String> talentos) {
        return cuerpoCorreo.replace("[GESTOR]", gestor.getNombres())
                .replace("[CLIENTE]", gestor.getCliente())
                .replace("[TIPO_FORMULARIO]", gestor.getTipoFormulario())
                .replace("{{listaTalentos}}", String.join("\n", talentos));
    }

    /**
     * Método genérico y de única responsabilidad para enviar correos con plantillas
     * HTML.
     *
     * @param to           El destinatario principal.
     * @param cc           Lista de destinatarios en copia (puede ser null o vacía).
     * @param subject      El asunto del correo.
     * @param templateName El nombre del archivo de la plantilla HTML (ej:
     *                     "requerimiento-template").
     * @param variables    Un mapa con las variables que se usarán en la plantilla.
     */
    @Async
    public void sendEmailWithHtmlTemplate(String to, List<String> cc, String subject, String templateName,
            Map<String, Object> variables) {
        try {
            // 1. Crear el contexto de Thymeleaf y cargar las variables
            Context context = new Context();
            context.setVariables(variables);

            // 2. Procesar la plantilla para obtener el cuerpo del correo en HTML
            String htmlBody = templateEngine.process(templateName, context);

            // 3. Crear y configurar el mensaje de correo
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(emisorCorreo);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (cc != null && !cc.isEmpty()) {
                helper.setCc(cc.toArray(new String[0]));
            }

            // 4. Enviar el correo
            mailSender.send(mimeMessage);
            logger.info("Correo enviado exitosamente a: {}", to);

        } catch (MessagingException e) {
            logger.error("Error al enviar correo a {}: {}", to, e.getMessage(), e);
        }
    }

}
