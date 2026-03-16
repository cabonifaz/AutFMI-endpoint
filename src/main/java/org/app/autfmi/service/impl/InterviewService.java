package org.app.autfmi.service.impl;

import org.app.autfmi.model.dto.InterviewResponseDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.InterviewListRequest;
import org.app.autfmi.model.request.InterviewRequest;
import org.app.autfmi.model.request.InterviewUpdateRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.InterviewDetailResponseDTO;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.model.response.PaginatedResponse;
import org.app.autfmi.repository.InterviewRepository;
import org.app.autfmi.util.ClientS3V2;
import org.app.autfmi.util.Constante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterviewService {

  private final InterviewRepository interviewRepository;
  private final ClientS3V2 clientS3;
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
  public OperationResult<InterviewDetailResponseDTO> getInterviewById(
      Integer idEntrevista,
      BaseRequest baseRequest) {
    return this.interviewRepository.getInterviewById(idEntrevista, baseRequest);
  }

  /**
   * Update Interview
   */
  public OperationResult<Void> updateInterview(
      InterviewUpdateRequest request,
      BaseRequest baseRequest) {
    return this.interviewRepository.updateInterview(request, baseRequest);
  }

  public OperationResult<Void> uploadInterviewFile(
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
  }

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
