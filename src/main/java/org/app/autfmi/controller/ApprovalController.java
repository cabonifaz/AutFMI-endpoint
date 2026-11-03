package org.app.autfmi.controller;

import org.app.autfmi.model.dto.aprobacion.ApprovalDetailDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalInboxDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalResult;
import org.app.autfmi.model.request.ApprovalListRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.service.impl.ApprovalService;
import org.app.autfmi.service.impl.ApprovalService.InvalidSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/approval")
@Tag(name = "Bandeja de aprobaciones")
@RequiredArgsConstructor
public class ApprovalController {

  private final ApprovalService approvalService;
  private final Logger logger = LoggerFactory.getLogger(ApprovalController.class);

  @GetMapping("/list")
  public ResponseEntity<OperationResult<ApprovalInboxDTO>> listRequests(
      @RequestParam(required = false) Integer idCliente,
      @RequestParam(required = false) String tipoSolicitud,
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) String busqueda,
      @RequestParam(required = false) Integer nPag,
      @RequestParam(required = false) Integer cPag,
      HttpServletRequest http) {
    try {
      ApprovalListRequest request = new ApprovalListRequest(idCliente, tipoSolicitud, estado, busqueda, nPag, cPag);
      return toResponse(approvalService.listRequests(bearerToken(http), request));
    } catch (InvalidSessionException e) {
      return unauthorized(e);
    } catch (Exception e) {
      this.logger.error("Error en listRequests: ", e);
      return serverError();
    }
  }

  @GetMapping("/detail/{idSolicitud}")
  public ResponseEntity<OperationResult<ApprovalDetailDTO>> getRequest(
      @PathVariable("idSolicitud") Integer idSolicitud,
      HttpServletRequest http) {
    try {
      return toResponse(approvalService.getRequest(bearerToken(http), idSolicitud));
    } catch (InvalidSessionException e) {
      return unauthorized(e);
    } catch (Exception e) {
      this.logger.error("Error en getRequest: ", e);
      return serverError();
    }
  }

  static String bearerToken(HttpServletRequest http) {
    String header = http.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      return null;
    }
    return header.substring("Bearer ".length()).trim();
  }

  static <T> ResponseEntity<OperationResult<T>> toResponse(ApprovalResult<T> result) {
    return ResponseEntity.status(result.outcome().httpStatus()).body(result.body());
  }

  static <T> ResponseEntity<OperationResult<T>> unauthorized(InvalidSessionException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new OperationResult<>(new BaseResponse(3, e.getMessage()), null));
  }

  private static <T> ResponseEntity<OperationResult<T>> serverError() {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new OperationResult<>(new BaseResponse(3, "Error al consultar la bandeja de aprobaciones"), null));
  }
}
