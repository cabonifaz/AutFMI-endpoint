package org.app.autfmi.util;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.mail.internet.MimeMessage;

/**
 * {@code JavaMailSender} que envía por Microsoft Graph en lugar de SMTP.
 *
 * <p>Los mensajes se siguen armando igual ({@code createMimeMessage} +
 * {@code MimeMessageHelper}); aquí cada {@link MimeMessage} se serializa y se
 * manda tal cual a {@code POST /users/{remitente}/sendMail} como MIME en
 * base64. Destinatarios, CC, cuerpo HTML, imágenes en línea y adjuntos viajan
 * sin cambios.</p>
 *
 * <p>Autenticación: credenciales de cliente de la app registrada en Entra ID
 * (permiso de aplicación {@code Mail.Send}). El token se reutiliza hasta poco
 * antes de vencer.</p>
 */
public class GraphMailSender extends JavaMailSenderImpl {

    private static final Logger logger = LoggerFactory.getLogger(GraphMailSender.class);

    private static final String GRAPH = "https://graph.microsoft.com/v1.0";
    private static final String SCOPE = "https://graph.microsoft.com/.default";
    // Límite de sendMail con MIME (petición completa). Por encima, Graph exige
    // borrador + subida por partes, que necesitaría además Mail.ReadWrite.
    private static final int MAX_MIME_BASE64 = 4 * 1024 * 1024;
    // Se renueva el token un poco antes de que venza.
    private static final Duration MARGEN_TOKEN = Duration.ofMinutes(5);

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String remitente;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    private String token;
    private Instant tokenVence = Instant.EPOCH;

    public GraphMailSender(String tenantId, String clientId, String clientSecret, String remitente) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.remitente = remitente;
    }

    @Override
    protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) throws MailException {
        Map<Object, Exception> fallidos = new LinkedHashMap<>();
        for (int i = 0; i < mimeMessages.length; i++) {
            Object original = originalMessages != null ? originalMessages[i] : mimeMessages[i];
            try {
                enviar(mimeMessages[i]);
            } catch (MailAuthenticationException e) {
                throw e;
            } catch (Exception e) {
                fallidos.put(original, e);
            }
        }
        if (!fallidos.isEmpty()) {
            throw new MailSendException(fallidos);
        }
    }

    private void enviar(MimeMessage message) throws Exception {
        message.saveChanges();
        ByteArrayOutputStream mime = new ByteArrayOutputStream();
        message.writeTo(mime);
        String base64 = Base64.getEncoder().encodeToString(mime.toByteArray());

        if (base64.length() > MAX_MIME_BASE64) {
            throw new MailSendException(String.format(
                    "El correo '%s' pesa %d KB y supera el límite de Microsoft Graph (4 MB con adjuntos).",
                    message.getSubject(), base64.length() / 1024));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAPH + "/users/" + URLEncoder.encode(remitente, StandardCharsets.UTF_8) + "/sendMail"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + token())
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(base64))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status == 202) {
            return;
        }
        if (status == 401) {
            // Token inválido o revocado: se fuerza a pedir uno nuevo en el siguiente envío.
            synchronized (this) {
                tokenVence = Instant.EPOCH;
            }
            throw new MailAuthenticationException("Graph rechazó el token (401): " + response.body());
        }
        if (status == 403) {
            throw new MailAuthenticationException("Sin permiso para enviar como " + remitente
                    + " (403). Revisar Mail.Send con consentimiento de administrador y la Application Access"
                    + " Policy del buzón: " + response.body());
        }
        throw new MailSendException("Graph sendMail respondió " + status + ": " + response.body());
    }

    /** Token de aplicación (client credentials), reutilizado hasta poco antes de vencer. */
    private synchronized String token() throws Exception {
        if (token != null && Instant.now().isBefore(tokenVence)) {
            return token;
        }
        String form = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8)
                + "&grant_type=client_credentials";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = mapper.readTree(response.body());
        if (response.statusCode() != 200 || !json.hasNonNull("access_token")) {
            // No se registra el cuerpo completo: puede traer datos de la app.
            String error = json.path("error").asText("") + " " + json.path("error_description").asText("");
            throw new MailAuthenticationException("No se pudo obtener el token de Microsoft Graph ("
                    + response.statusCode() + "): " + error.trim());
        }

        token = json.get("access_token").asText();
        long expiraEn = json.path("expires_in").asLong(3600);
        tokenVence = Instant.now().plusSeconds(expiraEn).minus(MARGEN_TOKEN);
        logger.info("Token de Microsoft Graph obtenido (vence en {} s); permisos de aplicación: {}.",
                expiraEn, rolesDelToken(token));
        return token;
    }

    /**
     * Permisos de aplicación concedidos (claim {@code roles} del token). Sin
     * {@code Mail.Send} aquí, falta el permiso de aplicación o su consentimiento
     * de administrador; si está y Graph responde 403, el bloqueo es la
     * Application Access Policy del buzón.
     */
    private String rolesDelToken(String jwt) {
        try {
            String[] partes = jwt.split("\\.");
            JsonNode payload = mapper.readTree(Base64.getUrlDecoder().decode(partes[1]));
            JsonNode roles = payload.path("roles");
            return roles.isMissingNode() ? "ninguno" : roles.toString();
        } catch (Exception e) {
            return "no se pudo leer";
        }
    }
}
