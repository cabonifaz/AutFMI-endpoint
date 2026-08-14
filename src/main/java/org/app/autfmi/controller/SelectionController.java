package org.app.autfmi.controller;

import java.util.List;

import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.dto.seleccion.SelectionInterviewsDTO;
import org.app.autfmi.model.dto.seleccion.SelectionLabelCountDTO;
import org.app.autfmi.model.dto.seleccion.SelectionPerformanceRowDTO;
import org.app.autfmi.model.dto.seleccion.SelectionSummaryDTO;
import org.app.autfmi.model.dto.seleccion.SelectionUserDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.SelectionFilterRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.service.impl.SelectionService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/seleccion")
@Tag(name = "Selección", description = "Estadísticas de entrevistas e ingresos")
@RequiredArgsConstructor
public class SelectionController {

  private final JwtHelper jwt;
  private final SelectionService selectionService;

  @PostMapping("/resumen")
  public ResponseEntity<OperationResult<SelectionSummaryDTO>> resumen(
      @RequestBody SelectionFilterRequest request, HttpServletRequest http) {
    try {
      BaseRequest baseRequest = buildBaseRequest(http, Constante.SELECCION_ESTADISTICAS);
      return ResponseEntity.ok(this.selectionService.getResumen(request, baseRequest));
    } catch (Exception e) {
      return ResponseEntity.ok(new OperationResult<>(new BaseResponse(3, "Error al obtener el resumen"), null));
    }
  }

  @PostMapping("/entrevistas")
  public ResponseEntity<OperationResult<SelectionInterviewsDTO>> entrevistas(
      @RequestBody SelectionFilterRequest request, HttpServletRequest http) {
    try {
      BaseRequest baseRequest = buildBaseRequest(http, Constante.SELECCION_ESTADISTICAS);
      return ResponseEntity.ok(this.selectionService.getEntrevistas(request, baseRequest));
    } catch (Exception e) {
      return ResponseEntity.ok(new OperationResult<>(new BaseResponse(3, "Error al obtener las entrevistas"), null));
    }
  }

  @PostMapping("/ingresos")
  public ResponseEntity<OperationResult<List<SelectionLabelCountDTO>>> ingresos(
      @RequestBody SelectionFilterRequest request, HttpServletRequest http) {
    try {
      BaseRequest baseRequest = buildBaseRequest(http, Constante.SELECCION_ESTADISTICAS);
      return ResponseEntity.ok(this.selectionService.getIngresos(request, baseRequest));
    } catch (Exception e) {
      return ResponseEntity.ok(new OperationResult<>(new BaseResponse(3, "Error al obtener los ingresos"), null));
    }
  }

  @PostMapping("/rendimiento")
  public ResponseEntity<OperationResult<List<SelectionPerformanceRowDTO>>> rendimiento(
      @RequestBody SelectionFilterRequest request, HttpServletRequest http) {
    try {
      BaseRequest baseRequest = buildBaseRequest(http, Constante.SELECCION_ESTADISTICAS);
      return ResponseEntity.ok(this.selectionService.getRendimiento(request, baseRequest));
    } catch (Exception e) {
      return ResponseEntity.ok(new OperationResult<>(new BaseResponse(3, "Error al obtener el rendimiento"), null));
    }
  }

  /** Buscador de usuarios de selección (desglose por-usuario). Solo Admin: gateado
   * por la funcionalidad SELECCION_USUARIOS, otorgada únicamente al rol 1. */
  @GetMapping("/usuarios")
  public ResponseEntity<OperationResult<List<SelectionUserDTO>>> usuarios(
      @RequestParam(required = false) String filtro, HttpServletRequest http) {
    try {
      BaseRequest baseRequest = buildBaseRequest(http, Constante.SELECCION_USUARIOS);
      return ResponseEntity.ok(this.selectionService.getUsuarios(filtro, baseRequest));
    } catch (Exception e) {
      return ResponseEntity.ok(new OperationResult<>(new BaseResponse(3, "Error al obtener los usuarios"), null));
    }
  }

  private BaseRequest buildBaseRequest(HttpServletRequest http, String funcionalidad) throws Exception {
    String token = JwtHelper.extractToken(http);
    UserDTO user = jwt.decodeToken(token);
    return Common.createBaseRequest(user, funcionalidad);
  }
}
