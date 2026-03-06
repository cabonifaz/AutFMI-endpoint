package org.app.autfmi.controller;

import org.app.autfmi.model.dto.InterviewResponseDTO;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.InterviewListRequest;
import org.app.autfmi.model.request.InterviewRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.model.response.PaginatedResponse;
import org.app.autfmi.service.impl.InterviewService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/interviews")
@Tag(name = "Interviews", description = "API para la gestión de entrevistas")
@RequiredArgsConstructor
public class InterviewController {

  private final JwtHelper jwt;
  private final InterviewService interviewService;

  @PostMapping("/create")
  public ResponseEntity<BaseResponse> createInterview(
      @RequestBody InterviewRequest interviewRequest,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);
      BaseRequest baseRequest = Common.createBaseRequest(user, Constante.CREATE_INTERVIEW);
      OperationResult<Integer> result = this.interviewService.createInterview(interviewRequest, baseRequest);
      return ResponseEntity.ok(result.getBaseResponse());
    } catch (Exception e) {
      return ResponseEntity.ok(new BaseResponse(3, "Error al obtener el token"));
    }
  }

  @PostMapping("/list")
  public ResponseEntity<OperationResult<PaginatedResponse<InterviewResponseDTO>>> listInterviews(
      @RequestBody InterviewListRequest interviewListRequest,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);
      BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LIST_INTERVIEW);
      var result = this.interviewService.listInterviews(interviewListRequest,
          baseRequest);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.status(500)
          .body(new OperationResult<>(new BaseResponse(3, "Error al listar las entrevistas"), null));
    }
  }

  @GetMapping("/detail/{id}")
  public ResponseEntity<?> getInterviewById(
      @PathVariable("id") Integer id,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);
      BaseRequest baseRequest = Common.createBaseRequest(user,
          Constante.LIST_INTERVIEW);

      var result = this.interviewService.getInterviewById(id, baseRequest);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.status(500)
          .body(new OperationResult<>(new BaseResponse(3, "Error al obtener el detalle de la entrevista"), null));
    }
  }

}
