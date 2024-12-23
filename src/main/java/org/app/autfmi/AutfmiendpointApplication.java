package org.app.autfmi;

import jakarta.mail.MessagingException;
import org.app.autfmi.util.Common;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AutfmiendpointApplication implements CommandLineRunner {

    @Autowired
    private Common utilitarios;

    public static void main(String[] args) {
        SpringApplication.run(AutfmiendpointApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        try {
            String htmlTemplate =
                    "<html>" +
                        "<head>" +
                            "<title>PDF de Prueba</title>" +
                            "<style>" +
                                "body {" +
                                    "font-family: Arial, sans-serif;" +
                                    "font-size: 14px;" +
                                    "color: #333;" +
                                    "background-color: #f4f4f4;" +
                                "}" +
                                "h1 {" +
                                    "color: #1e90ff;" +
                                "}" +
                                "p {" +
                                    "margin: 10px 0;" +
                                "}" +
                            "</style>" +
                        "</head>" +
                        "<body>" +
                            "<h1>Hola, {{nombre}}!</h1>" +
                            "<p>Tu saldo es: {{saldo}}.</p>" +
                            "<table>" +
                                "<th>Cabecera 1</th>" +
                                "<th>Cabecera 2</th>" +
                                "<tr>" +
                                    "<td>Columna 1</td>" +
                                    "<td>Celda 2</td>" +
                                "</tr>" +
                                "<tr>" +
                                    "<td>Columna 1</td>" +
                                    "<td>Celda 2</td>" +
                                "</tr>" +
                            "</table>" +
                        "</body>" +
                    "</html>";

            String nombre = "Usuario Test";
            double saldo = 1000.50;

            String htmlConDatos = utilitarios.reemplazarValores(htmlTemplate, nombre, saldo);

            utilitarios.enviarCorreoConPDF(
                    htmlConDatos,
                    "dial1901110514@gmail.com",
                    "Prueba correo",
                    """
                            Hola\s

                            Se realizó una actualización del talento, por favor revise el documento."""
            );
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Error al enviar el correo: " + e.getMessage());
        }
    }
}
