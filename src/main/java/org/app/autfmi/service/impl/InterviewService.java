package org.app.autfmi.service.impl;

import org.app.autfmi.model.response.InterviewFileResponse;
import org.app.autfmi.model.response.InterviewResponseDTO;
import org.app.autfmi.model.request.InterviewUploadConfirmRequest;
import org.app.autfmi.model.request.InterviewUploadUrlRequest;
import org.app.autfmi.model.request.InterviewDownloadFileRequest;
import org.app.autfmi.model.response.InterviewDownloadFileResponse;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.InterviewListRequest;
import org.app.autfmi.model.request.InterviewRequest;
import org.app.autfmi.model.request.InterviewUpdateRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.InterviewDetailResponseDTO;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.model.response.PaginatedResponse;
import org.app.autfmi.model.dto.TalentDTO;
import org.app.autfmi.model.dto.UserContactInfoDTO;
import org.app.autfmi.model.response.TalentResponse;
import org.app.autfmi.repository.InterviewRepository;
import org.app.autfmi.repository.TalentRepository;
import org.app.autfmi.repository.UserRepository;
import org.app.autfmi.service.IMailService;
import org.app.autfmi.util.ClientS3V2;
import org.app.autfmi.util.Constante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.app.autfmi.model.response.InterviewUploadUrlResponse;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewService {

  private final InterviewRepository interviewRepository;
  private final TalentRepository talentRepository;
  private final UserRepository userRepository;
  private final ClientS3V2 clientS3;
  private final IMailService mailService;
  private final Logger logger = LoggerFactory.getLogger(InterviewService.class);

  /**
   * Create Interview
   * 
   * @param request
   * @param baseRequest
   * @return
   */
  public OperationResult<Integer> createInterview(
      InterviewRequest request,
      BaseRequest baseRequest) {
    // El correo (con el ICS adjunto) se envía cuando el frontend confirma el ICS,
    // no aquí: al crear todavía no existe el ICS de la entrevista.
    return this.interviewRepository.createInterview(request, baseRequest);
  }

  /**
   * List Interviews
   * 
   * @param request
   * @param baseRequest
   * @return
   */
  public OperationResult<PaginatedResponse<InterviewResponseDTO>> listInterviews(
      InterviewListRequest request,
      BaseRequest baseRequest) {
    return this.interviewRepository.listInterviews(request, baseRequest);
  }

  /**
   * Get Interview Detail By ID
   * * @param idEntrevista
   * 
   * @param baseRequest
   * @return
   */
  public OperationResult<InterviewDetailResponseDTO> getInterviewById(Integer idEntrevista, BaseRequest baseRequest) {

    OperationResult<InterviewDetailResponseDTO> result = this.interviewRepository.getInterviewById(idEntrevista, baseRequest);

    return result;
  }

  /**
   * Update Interview
   */
  public OperationResult<Void> updateInterview(
      InterviewUpdateRequest request,
      BaseRequest baseRequest) {
    // El correo de actualización (con el ICS adjunto) se envía cuando el frontend
    // confirma el ICS regenerado, no aquí.
    return this.interviewRepository.updateInterview(request, baseRequest);
  }

  public OperationResult<InterviewUploadUrlResponse> generateUploadUrl(
    InterviewUploadUrlRequest request,
    BaseRequest baseRequest) {

      try {

        // 1. Validaciones básicas
        if (request.getFileName() == null || request.getFileName().trim().isEmpty()) {
          return new OperationResult<>(
              new BaseResponse(3, "Nombre de archivo inválido"),
              null);
        }

        // 2. Extraer extensión
        String originalFilename = request.getFileName();
        String extension = "";

        if (originalFilename.contains(".")) {
          extension =
              originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 3. Limpiar nombre visible
        String cleanName = originalFilename;

        if (cleanName.length() > 100) {
          cleanName = cleanName.substring(0, 95) + extension;
        }

        // 4. Nombre único en S3
        String generatedFileName =
            System.currentTimeMillis() + "_"
                + cleanName.replaceAll("\\s+", "_");

        // 5. Ruta S3
        String s3Path = new StringBuilder()
            .append(Constante.RUTA_REPOSITORIO)
            .append(baseRequest.getIdEmpresa())
            .append(Constante.RUTA_INTERVIEW.replace(
                "[ID_INTERVIEW]",
                request.getIdInterview().toString()))
            .append(generatedFileName)
            .toString();

        // 6. URL firmada PUT
        String uploadUrl =
            clientS3.generatePresignedUploadUrl(
                s3Path,
                request.getContentType(),
                5);

        // 7. Respuesta
        InterviewUploadUrlResponse response = new InterviewUploadUrlResponse();

        response.setUrl(uploadUrl);
        response.setPath(s3Path);
        response.setFileName(cleanName);

        return new OperationResult<>(
            new BaseResponse(2, "URL generada correctamente"),
            response);

      } catch (Exception e) {

        logger.error("Error generating upload URL", e);

        return new OperationResult<>(
            new BaseResponse(3, "Error generando URL"),
            null);
      }
    }

  public OperationResult<Void> confirmUpload(
      InterviewUploadConfirmRequest request,
      BaseRequest baseRequest) {

    try {

      // 1. Validar que exista físicamente en S3
      boolean exists = clientS3.exists(request.getPath());

      if (!exists) {
        return new OperationResult<>(
            new BaseResponse(3, "El archivo no existe en S3"),
            null);
      }

      boolean isIcs = request.getIdFileType() != null
          && request.getIdFileType() == Constante.TIPO_ARCHIVO_ENTREVISTA_ICS;

      // El ICS es único por entrevista: se capturan los ICS activos previos antes
      // de guardar el nuevo para reemplazarlos (lógico en BD + físico en S3).
      List<Integer> previousIcsIds = isIcs
          ? findActiveIcsFileIds(request.getIdInterview(), baseRequest)
          : new ArrayList<>();

      // 2. Guardar en BD
      OperationResult<Void> dbResult =
          interviewRepository.saveInterviewFile(
              request.getIdInterview(),
              request.getIdFileType(),
              request.getFileName(),
              request.getPath(),
              baseRequest);

      if (dbResult.getBaseResponse().getIdTipoMensaje() != 2) {
        return dbResult;
      }

      if (isIcs) {
        for (Integer oldId : previousIcsIds) {
          this.deleteInterviewFile(oldId, baseRequest);
        }
        if (Boolean.TRUE.equals(request.getNotify())) {
          dispatchIcsInterviewEmail(request, baseRequest);
        }
      }

      return dbResult;

    } catch (Exception e) {

      logger.error("Error confirmando archivo", e);

      return new OperationResult<>(
          new BaseResponse(3, "Error confirmando archivo"),
          null);
    }
  }

  /** Ids de los ICS activos de la entrevista (para reemplazarlos al subir uno nuevo). */
  private List<Integer> findActiveIcsFileIds(Integer idInterview, BaseRequest baseRequest) {
    List<Integer> ids = new ArrayList<>();
    OperationResult<InterviewDetailResponseDTO> detail =
        this.interviewRepository.getInterviewById(idInterview, baseRequest);
    if (detail != null && detail.getData() != null && detail.getData().getFiles() != null) {
      detail.getData().getFiles().stream()
          .filter(f -> f.getIdFileType() != null
              && f.getIdFileType() == Constante.TIPO_ARCHIVO_ENTREVISTA_ICS
              && f.getId() != null)
          .forEach(f -> ids.add(f.getId()));
    }
    return ids;
  }

  /**
   * Envía el correo de creación/actualización de la entrevista adjuntando el ICS
   * recién registrado. El adjunto es opcional (si no se puede leer de S3, el correo
   * se envía igualmente sin él). Nunca lanza: el correo no debe romper el registro.
   */
  private void dispatchIcsInterviewEmail(InterviewUploadConfirmRequest request, BaseRequest baseRequest) {
    try {
      OperationResult<InterviewDetailResponseDTO> detail =
          this.interviewRepository.getInterviewById(request.getIdInterview(), baseRequest);
      if (detail == null || detail.getData() == null || detail.getData().getIdTalento() == null) {
        return;
      }

      var talentResponse = this.talentRepository.getTalentById(detail.getData().getIdTalento(), baseRequest);
      if (!(talentResponse instanceof TalentResponse tr) || tr.getTalento() == null) {
        return;
      }
      TalentDTO talent = tr.getTalento();
      if (talent.getEmail() == null || talent.getEmail().trim().isEmpty()) {
        logger.info("Talent {} has no email registered, skipping interview notification",
            detail.getData().getIdTalento());
        return;
      }

      String talentFullName = talent.getNombres() + " " + talent.getApellidoPaterno()
          + (talent.getApellidoMaterno() != null ? " " + talent.getApellidoMaterno() : "");
      UserContactInfoDTO actionUserInfo = this.userRepository.getUserContactInfo(baseRequest);

      byte[] icsBytes = null;
      try {
        icsBytes = clientS3.download(request.getPath());
      } catch (Exception e) {
        logger.warn("No se pudo leer el ICS de S3 para adjuntarlo; se envía el correo sin adjunto: {}",
            e.getMessage());
      }

      String actionType = request.getNotificationType() != null
          ? request.getNotificationType()
          : "Nueva Entrevista";

      this.mailService.sendInterviewUnifiedNotification(
          detail.getData(), talent.getEmail().trim(), talentFullName.trim(),
          baseRequest, actionUserInfo, actionType, icsBytes, request.getFileName());

    } catch (Exception e) {
      logger.error("Error dispatching interview ICS notification: {}", e.getMessage(), e);
    }
  }

  public OperationResult<InterviewDownloadFileResponse> generateDownloadUrl(
        InterviewDownloadFileRequest request,
        BaseRequest baseRequest) {

    try {

        InterviewFileResponse file =
            interviewRepository.getFileById(request.getIdFile(), baseRequest);

        if (file == null || file.getPathFile() == null) {
            return new OperationResult<>(
                new BaseResponse(3, "Archivo no encontrado"),
                null
            );
          }

        String url = clientS3.generatePresignedUrl(
            file.getPathFile(),
            5
        );

        InterviewDownloadFileResponse response = new InterviewDownloadFileResponse();

        response.setUrl(url);
        response.setFileName(file.getFileName());

        return new OperationResult<>(
            new BaseResponse(2, "URL generada correctamente"),
            response
        );

    } catch (Exception e) {

        return new OperationResult<>(
            new BaseResponse(3, "Error generando URL de descarga"),
            
            null
        );
    }
  } 

  /*public OperationResult<Void> uploadInterviewFile(
      Integer idInterview,
      Integer idFileType,
      MultipartFile file,
      BaseRequest baseRequest) {

    String uploadedPath = null;

    try {
      // 1. Extraer nombre y extensión
      String originalFilename = file.getOriginalFilename();
      String extension = "";
      if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
      }

      // Limpiar nombre original para guardarlo "limpio" en la Base de Datos
      String cleanName = originalFilename != null ? originalFilename : "archivo" + extension;
      if (cleanName.length() > 100) {
        cleanName = cleanName.substring(0, 95) + extension;
      }

      // 2. Generar nombre de archivo único para S3 y armar la ruta con las Constantes
      // Ej: 1709765432123_CV_JuanPerez.pdf (se quitan espacios para evitar problemas
      // en URLs)
      String generatedFileName = System.currentTimeMillis() + "_" + cleanName.replaceAll("\\s+", "_");

      // Resultado Ej:
      // repositorio/1/interviews/1024/archivos/1709765432123_CV_JuanPerez.pdf
      String s3Path = new StringBuilder()
          .append(Constante.RUTA_REPOSITORIO)
          .append(baseRequest.getIdEmpresa())
          .append(Constante.RUTA_INTERVIEW.replace("[ID_INTERVIEW]", idInterview.toString()))
          .append(generatedFileName)
          .toString();

      // 3. Subir a S3 usando tu cliente
      uploadedPath = this.clientS3.upload(file, s3Path);
      this.logger.info("Archivo subido a S3 exitosamente en la ruta completa: {}", uploadedPath);

      // 4. Guardar en Base de Datos
      // Nota: Le pasamos 'cleanName' para que en la interfaz se vea el nombre normal,
      // pero le pasamos 'uploadedPath' para saber de dónde descargarlo.
      OperationResult<Void> dbResult = this.interviewRepository.saveInterviewFile(
          idInterview,
          idFileType,
          cleanName,
          uploadedPath,
          baseRequest);

      // 5. Rollback manual si falla la BD
      if (dbResult.getBaseResponse().getIdTipoMensaje() != 2) {
        this.logger.warn("Fallo el registro en BD, eliminando archivo de S3: {}", uploadedPath);
        this.clientS3.delete(uploadedPath);
      }

      return dbResult;

    } catch (Exception e) {
      this.logger.error("Fatal error uploading file: ", e);
      // Si falló a la mitad, intentamos limpiar la basura de S3
      if (uploadedPath != null) {
        try {
          this.clientS3.delete(uploadedPath);
        } catch (Exception ex) {
          this.logger.error("Error deleting file from S3: ", ex);
        }
      }
      return new OperationResult<>(new BaseResponse(3, "Error al subir el archivo: " + e.getMessage()), null);
    }
  }*/

  /**
   * Elimina el archivo lógicamente de la BD y físicamente de AWS S3.
   */
  public OperationResult<Void> deleteInterviewFile(Integer idArchivo, BaseRequest baseRequest) {
    // 1. Borramos de la BD y obtenemos la ruta
    OperationResult<String> dbResult = this.interviewRepository.deleteInterviewFile(idArchivo, baseRequest);

    // 2. Si la BD lo eliminó con éxito y nos devolvió una ruta, lo borramos de S3
    if (dbResult.getBaseResponse().getIdTipoMensaje() == 2 && dbResult.getData() != null) {
      String s3Path = dbResult.getData();
      try {
        this.logger.info("Procediendo a eliminar archivo físico de S3: {}", s3Path);
        this.clientS3.delete(s3Path);
      } catch (Exception e) {
        this.logger.error("El archivo se borró de la BD pero falló al eliminarse de S3 en la ruta: {}", s3Path, e);
      }
    }
    return new OperationResult<>(dbResult.getBaseResponse(), null);
  }

}
