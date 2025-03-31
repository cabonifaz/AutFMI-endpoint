package org.app.autfmi.util;

import jakarta.mail.internet.MimeMessage;
import org.app.autfmi.model.dto.GestorRqDTO;
import org.app.autfmi.model.dto.PostulantDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component

public class MailUtils {
    @Value("${spring.mail.username}")
    private String emisorCorreo;

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendRequirementPostulantMail(GestorRqDTO gestor, String asunto, List<PostulantDTO> lstPostulantes) {
        try {
            // lista de talentos al RQ
            List<String> listaTalentosRQ = new ArrayList<>();
            int indice = 1;

            for (PostulantDTO postulante: lstPostulantes) {
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
                        .replace("{{tieneEquipo}}", postulante.getTieneEquipo())
                );
                indice++;
            }

            String mensajeCorreo = replaceDataToHtmlBody(Constante.CUERPO_CORREO, gestor, listaTalentosRQ);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(emisorCorreo);
            helper.setTo(gestor.getCorreo());
            helper.setSubject(asunto);
            helper.setText(mensajeCorreo, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("ERROR AL ENVIAR CORREO");
            System.err.println(e.getMessage());
        }
    }

    private static String replaceDataToHtmlBody(String cuerpoCorreo, GestorRqDTO gestor, List<String> talentos){
        return cuerpoCorreo.replace("[GESTOR]", gestor.getNombres())
                .replace("[CLIENTE]", gestor.getCliente())
                .replace("{{listaTalentos}}", String.join("\n", talentos));
    }
}
