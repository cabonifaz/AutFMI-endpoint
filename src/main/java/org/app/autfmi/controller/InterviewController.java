package org.app.autfmi.controller;

import java.util.List;

import org.app.autfmi.model.response.InterviewResponseDTO;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.InterviewUploadConfirmRequest;
import org.app.autfmi.model.request.InterviewUploadUrlRequest;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.dto.InterviewByTalentDTO;
import org.app.autfmi.model.dto.InterviewQuestionDTO;
import org.app.autfmi.model.request.InterviewListRequest;
import org.app.autfmi.model.request.InterviewQuestionUpdateRequest;
import org.app.autfmi.model.request.InterviewQuestionsRequest;
import org.app.autfmi.model.request.InterviewRequest;
import org.app.autfmi.model.request.InterviewUpdateRequest;
import org.app.autfmi.model.request.InterviewDownloadFileRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
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
  public ResponseEntity<?> createInterview(
      @RequestBody InterviewRequest interviewRequest,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);
      BaseRequest baseRequest = Common.createBaseRequest(user, Constante.CREATE_INTERVIEW);
      // Se devuelve el OperationResult completo (incluye el id creado) para que el
      // frontend pueda generar y subir el ICS de la entrevista.
      OperationResult<Integer> result = this.interviewService.createInterview(interviewRequest, baseRequest);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.ok(new OperationResult<>(new BaseResponse(3, "Error al obtener el token"), null));
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

  @PostMapping("/update")
  public ResponseEntity<BaseResponse> updateInterview(
      @RequestBody InterviewUpdateRequest updateRequest,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);
      BaseRequest baseRequest = Common.createBaseRequest(user, Constante.UPDATE_INTERVIEW);

      OperationResult<Void> result = this.interviewService.updateInterview(updateRequest, baseRequest);
      return ResponseEntity.ok(result.getBaseResponse());
    } catch (Exception e) {
      return ResponseEntity.status(500).body(
          new BaseResponse(3, "Error de autenticación o token inválido"));
    }
  }

  /**
   * Entrevistas de un talento, opcionalmente de un solo tipo. La usa el atajo
   * "Entrevista telefónica" del detalle del talento para saber si ya tuvo una y
   * cuál fue la última.
   */
  @GetMapping("/by-talent")
  public ResponseEntity<?> listInterviewsByTalent(
      @RequestParam("idTalento") Integer idTalento,
      @RequestParam(value = "idTipoEntrevista", required = false) Integer idTipoEntrevista,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);
      BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LIST_INTERVIEW);

      OperationResult<List<InterviewByTalentDTO>> result =
          this.interviewService.listInterviewsByTalent(idTalento, idTipoEntrevista, baseRequest);

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.ok(
          new OperationResult<>(new BaseResponse(3, "Error al obtener el token"), null));
    }
  }

  // ─── Preguntas y respuestas (entrevista telefónica) ──────────────────────

  /**
   * Alta en bloque de las respuestas de una entrevista. Se llama después de
   * crearla —cuando ya hay id— igual que la subida del ICS.
   */
  @PostMapping("/questions")
  public ResponseEntity<BaseResponse> saveInterviewQuestions(
      @RequestBody InterviewQuestionsRequest request,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);
      BaseRequest baseRequest = Common.createBaseRequest(user, Constante.UPDATE_INTERVIEW);

      OperationResult<Void> result = this.interviewService.saveInterviewQuestions(
          request.getIdEntrevista(),
          request.getPreguntas(),
          baseRequest);

      return ResponseEntity.ok(result.getBaseResponse());
    } catch (Exception e) {
      return ResponseEntity.status(500).body(
          new BaseResponse(3, "Error de autenticación o token inválido"));
    }
  }

  /** Edición de una respuesta (habitualmente, su texto). */
  @PostMapping("/questions/update")
  public ResponseEntity<BaseResponse> updateInterviewQuestion(
      @RequestBody InterviewQuestionUpdateRequest request,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);
      BaseRequest baseRequest = Common.createBaseRequest(user, Constante.UPDATE_INTERVIEW);

      OperationResult<Void> result = this.interviewService.updateInterviewQuestion(
          request.getIdRespuesta(),
          request.getIdPregunta(),
          request.getRespuesta(),
          baseRequest);

      return ResponseEntity.ok(result.getBaseResponse());
    } catch (Exception e) {
      return ResponseEntity.status(500).body(
          new BaseResponse(3, "Error de autenticación o token inválido"));
    }
  }

  /** Baja lógica de una respuesta. */
  @PostMapping("/questions/remove")
  public ResponseEntity<BaseResponse> removeInterviewQuestion(
      @RequestParam("idRespuesta") Integer idRespuesta,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);
      BaseRequest baseRequest = Common.createBaseRequest(user, Constante.UPDATE_INTERVIEW);

      OperationResult<Void> result = this.interviewService.deleteInterviewQuestion(idRespuesta, baseRequest);

      return ResponseEntity.ok(result.getBaseResponse());
    } catch (Exception e) {
      return ResponseEntity.status(500).body(
          new BaseResponse(3, "Error de autenticación o token inválido"));
    }
  }

  /** Respuestas vigentes de una entrevista. */
  @GetMapping("/questions/{idEntrevista}")
  public ResponseEntity<?> listInterviewQuestions(
      @PathVariable("idEntrevista") Integer idEntrevista,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);
      BaseRequest baseRequest = Common.createBaseRequest(user, Constante.VIEW_INTERVIEW);

      OperationResult<List<InterviewQuestionDTO>> result =
          this.interviewService.listInterviewQuestions(idEntrevista, baseRequest);

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.ok(
          new OperationResult<>(new BaseResponse(3, "Error al obtener el token"), null));
    }
  }

  /*@PostMapping(value = "/file/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<BaseResponse> uploadInterviewFile(
      @RequestParam("idInterview") Integer idInterview,
      @RequestParam("idFileType") Integer idFileType,
      @RequestParam("file") MultipartFile file,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);
      BaseRequest baseRequest = Common.createBaseRequest(user, Constante.UPDATE_INTERVIEW);

      if (file.isEmpty()) {
        return ResponseEntity.badRequest().body(new BaseResponse(3, "El archivo está vacío."));
      }

      OperationResult<Void> result = this.interviewService.uploadInterviewFile(
          idInterview, idFileType, file, baseRequest);

      return ResponseEntity.ok(result.getBaseResponse());
    } catch (Exception e) {
      return ResponseEntity.status(500).body(
          new BaseResponse(3, "Error de autenticación o token inválido"));
    }
  }*/

  @PostMapping("/file/upload-url")
  public ResponseEntity<?> generateUploadUrl(
      @RequestBody InterviewUploadUrlRequest request,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);

      BaseRequest baseRequest =
          Common.createBaseRequest(user, Constante.UPLOAD_DOWNLOAD_INTERVIEW_FILE);

      var result = interviewService.generateUploadUrl(request, baseRequest);

      return ResponseEntity.ok(result);

    } catch (Exception e) {
      return ResponseEntity.status(500)
          .body(new BaseResponse(3, "Error generando URL de carga"));
    }
  }

  @PostMapping("/file/confirm-upload")
  public ResponseEntity<BaseResponse> confirmUpload(
      @RequestBody InterviewUploadConfirmRequest request,
      HttpServletRequest httpServletRequest) {

    try {

      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);

      BaseRequest baseRequest =
          Common.createBaseRequest(user, Constante.UPLOAD_DOWNLOAD_INTERVIEW_FILE);

      OperationResult<Void> result =
          interviewService.confirmUpload(request, baseRequest);

      return ResponseEntity.ok(result.getBaseResponse());

    } catch (Exception e) {
      return ResponseEntity.status(500)
          .body(new BaseResponse(3, "Error confirmando archivo"));
    }
  }

  @PostMapping("/file/remove")
  public ResponseEntity<BaseResponse> removeInterviewFile(
      @RequestParam("fileId") Integer fileId,
      HttpServletRequest httpServletRequest) {

    try {
      String token = JwtHelper.extractToken(httpServletRequest);
      UserDTO user = jwt.decodeToken(token);

      BaseRequest baseRequest = Common.createBaseRequest(user, Constante.UPLOAD_DOWNLOAD_INTERVIEW_FILE);

      OperationResult<Void> result = this.interviewService.deleteInterviewFile(
          fileId,
          baseRequest);

      return ResponseEntity.ok(result.getBaseResponse());
    } catch (Exception e) {
      return ResponseEntity.status(500).body(
          new BaseResponse(3, "Error de autenticación o token inválido"));
    }
  }
  
  @PostMapping("/file/download-url")
  public ResponseEntity<?> downloadUrl(
      @RequestBody InterviewDownloadFileRequest request,
      HttpServletRequest httpServletRequest
  ) {
      try {

        String token = JwtHelper.extractToken(httpServletRequest);
        UserDTO user = jwt.decodeToken(token);

        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.UPLOAD_DOWNLOAD_INTERVIEW_FILE);

        return ResponseEntity.ok(interviewService.generateDownloadUrl(request, baseRequest));

      } catch (Exception e) {

        return ResponseEntity.status(500)
            .body(new BaseResponse(3, "Error generando URL de descarga"));
      }
      
  }
}
