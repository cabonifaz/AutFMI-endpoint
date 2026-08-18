package org.app.autfmi.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

import org.app.autfmi.model.dto.FileDTO;
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

import org.springframework.core.io.Resource;

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

    @Async("notificationExecutor")
    public void sendRequirementPostulantMail(GestorRqDTO gestor, String asunto, List<PostulantDTO> lstPostulantes,
            List<String> lstEmails, List<FileDTO> attachments) {
        try {
            // lista de talentos al RQ
            List<String> listaTalentosRQ = new ArrayList<>();
            int indice = 1;

            for (PostulantDTO postulante : lstPostulantes) {
                listaTalentosRQ.add(Constante.LIST_TALENT_ROW
                        .replace("{{numFila}}", SafeValues.safeString(indice + ""))
                        .replace("{{nombres}}", SafeValues.safeString(postulante.getNombres()))
                        .replace("{{apellidos}}", SafeValues.safeString(postulante.getApellidos()))
                        .replace("{{docIdentidad}}", SafeValues.safeString(postulante.getDni()))
                        .replace("{{numCelular}}", SafeValues.safeString(postulante.getCelular()))
                        .replace("{{correo}}", SafeValues.safeString(postulante.getEmail()))
                        .replace("{{fchInicioLabores}}", SafeValues.safeString(postulante.getFechaInicioLabores()))
                        .replace("{{tiempoContrato}}", SafeValues.safeString(postulante.getTiempoContrato()))
                        .replace("{{cargo}}", SafeValues.safeString(postulante.getCargo()))
                        .replace("{{remuneracion}}", SafeValues.safeString(postulante.getRemuneracion().toString()))
                        .replace("{{modalidad}}", SafeValues.safeString(postulante.getModalidad()))
                        .replace("{{tieneEquipo}}", SafeValues.safeString(postulante.getTieneEquipo())));
                indice++;
            }

            String mensajeCorreo = replaceDataToHtmlBody(Constante.CUERPO_CORREO, gestor, listaTalentosRQ);
            String asuntoCorreo = asunto + " | " + gestor.getCodigoRQ() + " | " + gestor.getCliente();

            // El destinatario principal es el usuario que ejecutó la acción; sin un
            // correo válido no tiene sentido continuar.
            String to = gestor.getCorreo() != null ? gestor.getCorreo().trim() : null;
            if (!EmailUtils.isValidEmail(to)) {
                logger.error("Correo cancelado: el usuario destino no tiene un correo válido ('{}')", to);
                return;
            }

            // Descartamos CC nulos/vacíos, con formato inválido, duplicados y el propio TO.
            List<String> cleanCc = EmailUtils.sanitizeRecipients(lstEmails, to);

            try {
                enviarMensajeHtml(to, cleanCc, asuntoCorreo, mensajeCorreo, attachments);
                logger.info("Email sent to: {} (cc {}: {})", to, cleanCc.size(), cleanCc);
            } catch (Exception e) {
                // Un CC inválido a nivel SMTP no debe impedir que el actuador reciba su correo.
                logger.error("Falló el envío con CC ({}); reintentando solo al destinatario principal",
                        e.getMessage());
                enviarMensajeHtml(to, List.of(), asuntoCorreo, mensajeCorreo, attachments);
                logger.info("Email sent to: {} (sin CC tras reintento)", to);
            }
        } catch (Exception e) {
            logger.error("Email not sent to: {}", gestor.getCorreo());
            logger.error("Error al enviar correo: {}", e.getMessage(), e);
        }
    }

    /**
     * Construye y envía un mensaje HTML simple. Se aísla en su propio método para
     * poder reintentar el envío (por ejemplo, sin CC) reconstruyendo el mensaje.
     */
    private void enviarMensajeHtml(String to, List<String> cc, String subject, String htmlBody,
            List<FileDTO> attachments)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(emisorCorreo);
        helper.setTo(to);
        if (cc != null && !cc.isEmpty()) {
            helper.setCc(cc.toArray(new String[0]));
        }
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        if (attachments != null) {
            for (FileDTO objfile : attachments) {
                if (objfile != null && objfile.getByteArchivo() != null) {
                    ByteArrayDataSource dataSource = new ByteArrayDataSource(objfile.getByteArchivo(), "application/pdf");
                    helper.addAttachment(objfile.getNombreArchivo() + ".pdf", dataSource);
                }
            }
        }

        mailSender.send(message);
    }

    private static String replaceDataToHtmlBody(String cuerpoCorreo, GestorRqDTO gestor, List<String> talentos) {

        var tipoFormulario = gestor.getTipoFormulario() != null ? gestor.getTipoFormulario() : "Ingreso";

        return cuerpoCorreo.replace("[GESTOR]", gestor.getNombres())
                .replace("[CLIENTE]", gestor.getCliente())
                .replace("[TIPO_FORMULARIO]", tipoFormulario)
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
    @Async("notificationExecutor")
    public void sendEmailWithHtmlTemplate(String to, List<String> cc, String subject, String templateName,
            Map<String, Object> variables) {
        try {
            // 1. Crear el contexto de Thymeleaf y cargar las variables
            Context context = new Context();
            context.setVariables(variables);

            // 2. Procesar la plantilla para obtener el cuerpo del correo en HTML
            String htmlBody = templateEngine.process(templateName, context);

            // 3. Validar destinatario principal y depurar la lista de CC
            String destino = to != null ? to.trim() : null;
            if (!EmailUtils.isValidEmail(destino)) {
                logger.error("Correo cancelado: destinatario principal inválido ('{}')", to);
                return;
            }
            List<String> cleanCc = EmailUtils.sanitizeRecipients(cc, destino);

            // 4. Enviar el correo; un CC inválido no debe bloquear el envío al TO
            try {
                enviarMensajeHtml(destino, cleanCc, subject, htmlBody, null);
                logger.info("Correo enviado exitosamente a: {} (cc: {})", destino, cleanCc.size());
            } catch (MessagingException e) {
                logger.error("Falló el envío con CC ({}); reintentando solo al destinatario principal",
                        e.getMessage());
                enviarMensajeHtml(destino, List.of(), subject, htmlBody, null);
                logger.info("Correo enviado exitosamente a: {} (sin CC tras reintento)", destino);
            }

        } catch (MessagingException e) {
            logger.error("Error al enviar correo a {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendEmailWithHtmlTemplate(String to, List<String> cc, String subject, String templateName,
            Map<String, Object> variables, Map<String, Resource> inlineResources) {
        sendEmailWithHtmlTemplate(to, cc, subject, templateName, variables, inlineResources, null, null);
    }

    /**
     * Variante SÍNCRONA que devuelve si el correo se envió. Se usa desde procesos
     * batch (p. ej. el job de notificación de inicio de labores) donde hay que
     * confirmar el envío antes de marcar el registro como notificado. Un CC
     * inválido a nivel SMTP no bloquea el envío: se reintenta solo al TO.
     *
     * @return {@code true} si el correo llegó a enviarse (con o sin CC);
     *         {@code false} si el destinatario es inválido o el envío falló.
     */
    public boolean sendTemplateEmailSync(String to, List<String> cc, String subject, String templateName,
            Map<String, Object> variables) {
        String destino = to != null ? to.trim() : null;
        if (!EmailUtils.isValidEmail(destino)) {
            logger.error("Correo '{}' cancelado: destinatario principal inválido ('{}')", subject, to);
            return false;
        }
        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlBody = templateEngine.process(templateName, context);
            List<String> cleanCc = EmailUtils.sanitizeRecipients(cc, destino);
            try {
                enviarMensajeHtml(destino, cleanCc, subject, htmlBody, null);
                logger.info("Correo '{}' enviado a: {} (cc: {})", subject, destino, cleanCc.size());
            } catch (MessagingException e) {
                logger.error("Falló el envío de '{}' con CC ({}); reintentando solo al destinatario",
                        subject, e.getMessage());
                enviarMensajeHtml(destino, List.of(), subject, htmlBody, null);
                logger.info("Correo '{}' enviado a: {} (sin CC tras reintento)", subject, destino);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error al enviar correo '{}' a {}: {}", subject, destino, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Variante que además adjunta un archivo (p. ej. el ICS de la entrevista). El
     * adjunto es opcional: si {@code attachmentBytes} es null/vacío, se envía sin él.
     */
    public void sendEmailWithHtmlTemplate(String to, List<String> cc, String subject, String templateName,
            Map<String, Object> variables, Map<String, Resource> inlineResources,
            String attachmentName, byte[] attachmentBytes) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlBody = templateEngine.process(templateName, context);

            String destino = to != null ? to.trim() : null;
            if (!EmailUtils.isValidEmail(destino)) {
                logger.error("Correo cancelado: destinatario principal inválido ('{}')", to);
                return;
            }
            List<String> cleanCc = EmailUtils.sanitizeRecipients(cc, destino);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(emisorCorreo);
            helper.setTo(destino);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (!cleanCc.isEmpty()) {
                helper.setCc(cleanCc.toArray(new String[0]));
            }

            if (inlineResources != null) {
                for (Map.Entry<String, Resource> entry : inlineResources.entrySet()) {
                    try {
                        helper.addInline(entry.getKey(), entry.getValue());
                    } catch (Exception e) {
                        logger.warn("Could not embed inline resource '{}': {}", entry.getKey(), e.getMessage());
                    }
                }
            }

            if (attachmentBytes != null && attachmentBytes.length > 0 && attachmentName != null) {
                try {
                    helper.addAttachment(attachmentName,
                            new org.springframework.core.io.ByteArrayResource(attachmentBytes));
                } catch (Exception e) {
                    logger.warn("Could not attach '{}': {}", attachmentName, e.getMessage());
                }
            }

            mailSender.send(mimeMessage);
            logger.info("Correo enviado exitosamente a: {}", to);

        } catch (MessagingException e) {
            logger.error("Error al enviar correo a {}: {}", to, e.getMessage(), e);
        }
    }

}
