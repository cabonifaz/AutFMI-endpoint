package org.app.autfmi.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.app.autfmi.model.dto.GestorRqDTO;
import org.app.autfmi.model.dto.PostulantDTO;
import org.app.autfmi.model.dto.RequirementDTO;
import org.app.autfmi.model.dto.RequirementVacanteDTO;
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
import java.util.HashMap;
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

    @Async
    public void sendRequirementNotification(
            RequirementDTO rDto,
            String dest, Map<String, Object> rqData, List<PostulantDTO> lstPostulantes,
            List<String> lstEmailsCC) {

        Map<String, Object> variables = new HashMap<>();

        String asunto = "Creación de Requerimiento";

        // ==========================
        // Datos del requerimiento
        // ==========================
        Map<String, Object> rqDataT = new HashMap<>();
        rqDataT.put("codigo", rDto.getCodigoRQ());
        rqDataT.put("titulo", rDto.getTitulo());
        rqDataT.put("descripcion", rDto.getDescripcion());
        rqDataT.put("fechaSolicitud", rDto.getFechaSolicitud());
        rqDataT.put("fechaVencimiento", rDto.getFechaVencimiento());
        variables.put("rqData", rqDataT);

        // ==========================
        // Datos de gestión
        // ==========================
        Map<String, Object> gestion = new HashMap<>();
        gestion.put("duracion", rDto.getDuracion() + " meses");
        gestion.put("modalidad", "Remoto");
        gestion.put("modalidadesFacturacion", List.of("Planilla - Régimen General", "Recibo por Honorarios"));
        variables.put("gestion", gestion);

        // ==========================
        // Datos del cliente y contactos
        // ==========================
        Map<String, Object> cliente = new HashMap<>();
        cliente.put("nombre", "Banco Interamericano de Finanzas - BanBif");

        List<Map<String, Object>> contactos = new ArrayList<>();

        Map<String, Object> contacto1 = new HashMap<>();
        contacto1.put("nombre", "Denis");
        contacto1.put("apellido", "Alcántara Laura");
        contacto1.put("celular", "999000111");
        contacto1.put("correo", "denis.alcantara@banbif.com");
        contacto1.put("cargo", "Jefe de Tecnología");
        contactos.add(contacto1);

        Map<String, Object> contacto2 = new HashMap<>();
        contacto2.put("nombre", "Lucía");
        contacto2.put("apellido", "Ramos Fernández");
        contacto2.put("celular", "988123456");
        contacto2.put("correo", "lucia.ramos@banbif.com");
        contacto2.put("cargo", "Gerente de RRHH");
        contactos.add(contacto2);

        cliente.put("contactos", contactos);
        variables.put("cliente", cliente);

        // ==========================
        // Vacantes del requerimiento
        // ==========================
        List<Map<String, Object>> vacantes = new ArrayList<>();

        for (RequirementVacanteDTO v : rDto.getLstRqVacantes()) {
            vacantes.add(new HashMap<String, Object>() {
                {
                    put("perfil", v.getPerfilProfesional());
                    put("cantidad", v.getCantidad());
                    put("tarifaFinal", "S/. " + v.getTarifaFinal().toString());
                    put("tipoTarifa", "Por hora");
                }
            });

        }

        variables.put("vacantes", vacantes);

        // ==========================
        // Postulantes
        // ==========================
        List<Map<String, Object>> postulantes = new ArrayList<>();

        Map<String, Object> postulante1 = new HashMap<>();
        postulante1.put("nombre", "José Pérez");
        postulante1.put("correo", "jose.perez@fractal.com");
        postulante1.put("perfil", "Backend Developer");
        postulante1.put("estado", "En revisión");
        postulantes.add(postulante1);

        Map<String, Object> postulante2 = new HashMap<>();
        postulante2.put("nombre", "María Torres");
        postulante2.put("correo", "maria.torres@fractal.com");
        postulante2.put("perfil", "QA Automation Engineer");
        postulante2.put("estado", "Preseleccionada");
        postulantes.add(postulante2);

        variables.put("postulantes", postulantes);

        // src/main/resources/templates/
        sendEmailWithHtmlTemplate(
                dest,
                lstEmailsCC,
                asunto,
                "rq-details",
                variables);
    }

}
