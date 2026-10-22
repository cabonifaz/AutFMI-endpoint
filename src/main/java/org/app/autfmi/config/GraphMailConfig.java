package org.app.autfmi.config;

import org.app.autfmi.util.GraphMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Envío de correo por Microsoft Graph (cuenta de Microsoft 365) cuando
 * {@code mail.provider=graph} (env {@code MAIL_PROVIDER}). Con cualquier otro
 * valor no se crea este bean y Spring Boot configura el SMTP de siempre
 * ({@code spring.mail.*}).
 */
@Configuration
@ConditionalOnProperty(name = "mail.provider", havingValue = "graph")
public class GraphMailConfig {

    private static final Logger logger = LoggerFactory.getLogger(GraphMailConfig.class);

    @Bean
    public JavaMailSender graphMailSender(
            @Value("${microsoft.graph.tenant-id}") String tenantId,
            @Value("${microsoft.graph.client-id}") String clientId,
            @Value("${microsoft.graph.client-secret}") String clientSecret,
            @Value("${spring.mail.username}") String remitente,
            @Value("${microsoft.graph.sender:}") String buzon) {
        if (tenantId.isBlank() || clientId.isBlank() || clientSecret.isBlank() || remitente.isBlank()) {
            throw new IllegalStateException("MAIL_PROVIDER=graph requiere MS_TENANT_ID, MS_CLIENT_ID,"
                    + " MS_CLIENT_SECRET y MAIL_USERNAME (buzón remitente).");
        }
        // Buzón en la URL de Graph: UPN o Id. de objeto. Si no se indica, se usa
        // MAIL_USERNAME (sirve cuando el UPN coincide con la dirección de correo).
        String buzonGraph = buzon.isBlank() ? remitente : buzon.trim();
        logger.info("Correo por Microsoft Graph como {} (buzón {}).", remitente, buzonGraph);
        return new GraphMailSender(tenantId, clientId, clientSecret, buzonGraph);
    }
}
