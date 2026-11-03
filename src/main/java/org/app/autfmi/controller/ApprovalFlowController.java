package org.app.autfmi.controller;

import static org.app.autfmi.controller.ApprovalController.bearerToken;
import static org.app.autfmi.controller.ApprovalController.toResponse;
import static org.app.autfmi.controller.ApprovalController.unauthorized;

import org.app.autfmi.model.dto.aprobacion.ApprovalDecisionDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalFlowDTO;
import org.app.autfmi.model.request.ApprovalApproveRequest;
import org.app.autfmi.model.request.ApprovalFormUpdateRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.service.impl.ApprovalService.InvalidSessionException;
import org.app.autfmi.service.impl.ApprovalWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** Flujo de aprobación de una solicitud de la bandeja (FTBO-1124). */
@RestController
@RequestMapping("/approval")
@Tag(name = "Bandeja de aprobaciones")
@RequiredArgsConstructor
public class ApprovalFlowController {

  private static final String MENSAJE_ERROR = "Error al procesar la solicitud de aprobación";

  private final ApprovalWorkflowService workflowService;
  private final Logger logger = LoggerFactory.getLogger(ApprovalFlowController.class);

  /** Permisos del usuario, versión, campos editables y línea de tiempo. */
  @GetMapping("/{idSolicitud}/flow")
  public ResponseEntity<OperationResult<ApprovalFlowDTO>> getFlow(
      @PathVariable("idSolicitud") Integer idSolicitud,
      HttpServletRequest http) {
    try {
      return toResponse(workflowService.getFlow(bearerToken(http), idSolicitud));
    } catch (InvalidSessionException e) {
      return unauthorized(e);
    } catch (Exception e) {
      this.logger.error("Error en getFlow: ", e);
      return serverError();
    }
  }

  /** Guarda los campos editados por un gestor asignado. 409 si otro cambió la solicitud. */
  @PutMapping("/{idSolicitud}/form")
  public ResponseEntity<OperationResult<ApprovalDecisionDTO>> updateForm(
      @PathVariable("idSolicitud") Integer idSolicitud,
      @RequestBody(required = false) ApprovalFormUpdateRequest request,
      HttpServletRequest http) {
    try {
      return toResponse(workflowService.updateForm(bearerToken(http), idSolicitud, request));
    } catch (InvalidSessionException e) {
      return unauthorized(e);
    } catch (Exception e) {
      this.logger.error("Error en updateForm: ", e);
      return serverError();
    }
  }

  /** Aprueba el flujo completo. Solo un gestor asignado y una sola vez (409 para los demás). */
  @PostMapping("/{idSolicitud}/approve")
  public ResponseEntity<OperationResult<ApprovalDecisionDTO>> approve(
      @PathVariable("idSolicitud") Integer idSolicitud,
      @RequestBody(required = false) ApprovalApproveRequest request,
      HttpServletRequest http) {
    try {
      return toResponse(workflowService.approve(bearerToken(http), idSolicitud, request));
    } catch (InvalidSessionException e) {
      return unauthorized(e);
    } catch (Exception e) {
      this.logger.error("Error en approve: ", e);
      return serverError();
    }
  }

  private static <T> ResponseEntity<OperationResult<T>> serverError() {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new OperationResult<>(new BaseResponse(3, MENSAJE_ERROR), null));
  }
}
