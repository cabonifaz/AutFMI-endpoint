package org.app.autfmi.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.app.autfmi.model.request.EmailConfirmationRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.service.IRequirementConfirmationService;
import org.app.autfmi.util.Constante;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Confirmación del ingreso por correo. Lo llama n8n (no un usuario), por eso no
 * usa JWT sino una API key en la cabecera {@value Constante#HEADER_API_KEY}.
 *
 * <p>Respuestas para n8n:
 * <ul>
 * <li>200 + idTipoMensaje 2: confirmado y formularios enviados.</li>
 * <li>200 + idTipoMensaje 1: no había nada pendiente (ya se confirmó antes). No reintentar.</li>
 * <li>200 + idTipoMensaje 3: rechazado (RQ/código no válidos o remitente no autorizado). No reintentar.</li>
 * <li>401: API key ausente o inválida.</li>
 * <li>503: confirmación válida, pero los formularios no salieron; quedaron pendientes. Reintentar.</li>
 * <li>500: error inesperado. Reintentar.</li>
 * </ul>
 */
@RestController
@RequestMapping("/requirement/confirmation")
@RequiredArgsConstructor
@Tag(name = "Confirmación de ingreso (n8n)")
public class RequirementConfirmationController {

    private final IRequirementConfirmationService confirmationService;

    @Value("${n8n.api-key:}")
    private String apiKey;

    @PostMapping
    public ResponseEntity<BaseResponse> confirmByEmail(
            @RequestHeader(value = Constante.HEADER_API_KEY, required = false) String requestApiKey,
            @RequestBody EmailConfirmationRequest request) {

        if (!isValidApiKey(requestApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BaseResponse(3, "API key ausente o inválida"));
        }

        try {
            BaseResponse response = confirmationService.processEmailConfirmation(request);
            HttpStatus status = Constante.CONFIRMACION_REINTENTAR.equals(response.getDetalleMensaje())
                    ? HttpStatus.SERVICE_UNAVAILABLE
                    : HttpStatus.OK;
            return ResponseEntity.status(status).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse(3, e.getMessage()));
        }
    }

    /**
     * Sin API key configurada se rechaza todo (falla cerrado). La comparación es
     * en tiempo constante para no filtrar la clave por tiempos de respuesta.
     */
    private boolean isValidApiKey(String requestApiKey) {
        if (apiKey == null || apiKey.isBlank() || requestApiKey == null) {
            return false;
        }
        return MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8),
                requestApiKey.getBytes(StandardCharsets.UTF_8));
    }
}
