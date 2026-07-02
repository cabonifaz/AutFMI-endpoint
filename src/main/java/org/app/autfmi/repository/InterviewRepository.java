package org.app.autfmi.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.EntrevistadorDTO;
import org.app.autfmi.model.dto.GrabacionDTO;
import org.app.autfmi.model.dto.InterviewFileDTO;
import org.app.autfmi.model.response.InterviewFileResponse;
import org.app.autfmi.model.response.InterviewResponseDTO;
import org.app.autfmi.model.dto.InterviewRqDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.InterviewListRequest;
import org.app.autfmi.model.request.InterviewRequest;
import org.app.autfmi.model.request.InterviewUpdateRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.InterviewDetailResponseDTO;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.model.response.PaginatedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class InterviewRepository {

  @org.springframework.lang.NonNull
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final Logger logger = LoggerFactory.getLogger(InterviewRepository.class);

  /**
   * Ejecuta el SP_ENTREVISTAS_INS para registrar una nueva entrevista.
   * 
   * @param request     Datos de la entrevista
   * @param baseRequest Datos de la auditoría
   * @return OperationResult con el estado de la operación
   */
  public OperationResult<Integer> createInterview(InterviewRequest request, BaseRequest baseRequest) {

    SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
        .withProcedureName("SP_ENTREVISTAS_INS");

    BaseResponse baseResponse;

    try {
      String lstIdReqJson = objectMapper.writeValueAsString(request.getLstIdRequerimientos());

      var params = new MapSqlParameterSource()
          .addValue("ID_TALENTO", request.getIdTalento())
          .addValue("FECHA", request.getFecha())
          .addValue("HORA", request.getHora())
          .addValue("ID_ESTADO", request.getEstado())
          .addValue("ID_ETAPA", request.getEtapa())
          .addValue("ENLACE_ENTREVISTA", request.getEnlaceEntrevista())
          .addValue("LST_ID_RQS", lstIdReqJson)
          .addValue("LST_ENTREVISTADORES", request.getEntrevistadores())
          .addValue("USUARIO", baseRequest.getUsername())
          .addValue("ID_USUARIO", baseRequest.getIdUsuario())
          .addValue("ID_ROL", baseRequest.getIdRol())
          .addValue("FUNCIONALIDADES", baseRequest.getFuncionalidades())
          .addValue("PERFIL", request.getPerfil());

      // Ejecución
      Map<String, Object> result = simpleJdbcCall.execute(params);

      // Extracción manual del Result Set (Estándar solicitado)
      List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

      if (resultSet == null || resultSet.isEmpty()) {
        baseResponse = new BaseResponse(3, "La base de datos no retornó información.");
        return new OperationResult<>(baseResponse, null);
      }

      Map<String, Object> row = resultSet.get(0);
      Integer messageId = (Integer) row.get("ID_TIPO_MENSAJE");
      String message = (String) row.get("MENSAJE");
      Integer operationId = (Integer) row.get("ID_OPERACION");

      baseResponse = new BaseResponse(messageId, message);
      return new OperationResult<>(baseResponse, operationId);

    } catch (JsonProcessingException e) {
      this.logger.error("Error al serializar requerimientos a JSON", e);
      return new OperationResult<>(new BaseResponse(3, "Error de formato en requerimientos"), null);
    } catch (Exception e) {
      this.logger.error("Error crítico en createInterview: ", e);
      return new OperationResult<>(new BaseResponse(3, "Error interno: " + e.getMessage()), null);
    }
  }

  /**
   * Ejecuta el SP_ENTREVISTA_LST para listar las entrevistas.
   * * @param request Filtros de búsqueda
   * 
   * @param baseRequest Datos de auditoría y permisos
   * @return OperationResult con la lista paginada de entrevistas
   */
  public OperationResult<PaginatedResponse<InterviewResponseDTO>> listInterviews(InterviewListRequest request,
      BaseRequest baseRequest) {

    SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
        .withProcedureName("SP_ENTREVISTA_LST");

    try {
      java.util.function.Function<String, String> clean = s -> (s == null || s.trim().isEmpty()) ? null : s.trim();

      var params = new MapSqlParameterSource()
          .addValue("ID_ROL", baseRequest.getIdRol())
          .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
          .addValue("ID_USUARIO", baseRequest.getIdUsuario())
          .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
          .addValue("N_PAG", request.getNPag())
          .addValue("BUSQUEDA", clean.apply(request.getBusqueda()))
          .addValue("ID_CLIENTE", request.getIdCliente())
          .addValue("ID_ESTADO", request.getIdEstado())
          .addValue("ID_ETAPA", request.getIdEtapa())//Lo agrego Franco xd
          .addValue("FECHA", clean.apply(request.getFecha()));

      this.logger.info("Getting interviews for: {}", baseRequest.getUsername());



      Map<String, Object> result = simpleJdbcCall.execute(params);
      List<Map<String, Object>> rs1 = (List<Map<String, Object>>) result.get("#result-set-1");

      if (rs1 == null || rs1.isEmpty()) {
        this.logger.error("No response from database");
        return new OperationResult<>(new BaseResponse(3, "Sin respuesta del servidor"), null);
      }

      Map<String, Object> baseRow = rs1.get(0);
      Integer messageId = (Integer) baseRow.get("ID_TIPO_MENSAJE");
      String message = (String) baseRow.get("MENSAJE");

      BaseResponse baseResponse = new BaseResponse(messageId, message);

      if (messageId == 2) {
        List<Map<String, Object>> rs2 = (List<Map<String, Object>>) result.get("#result-set-2");
        List<InterviewResponseDTO> list = new ArrayList<>();

        // Variables por defecto si la lista está vacía
        int totalElements = 0;
        int totalPages = 0;
        int currentPage = request.getNPag() != null ? request.getNPag() : 1;

        if (rs2 != null && !rs2.isEmpty()) {
          // Extraer metadata de paginación de la primera fila
          Map<String, Object> firstRow = rs2.get(0);
          totalElements = firstRow.get("TOTAL_LISTA") != null ? ((Number) firstRow.get("TOTAL_LISTA")).intValue() : 0;
          int pageSize = firstRow.get("TAMANO_PAGINA") != null ? ((Number) firstRow.get("TAMANO_PAGINA")).intValue()
              : 10;

          // Calcular el total de páginas
          totalPages = (int) Math.ceil((double) totalElements / pageSize);

          // Mapear la lista de DTOs
          for (Map<String, Object> row : rs2) {
            list.add(InterviewResponseDTO.builder()
                .id(row.get("ID") != null ? ((Number) row.get("ID")).intValue() : null)
                .talento((String) row.get("TALENTO"))
                .tituloRq((String) row.get("TITULO_RQ"))
                .cliente((String) row.get("CLIENTE"))
                .fechaEntrevista(row.get("FECHA_ENTREVISTA") != null ? row.get("FECHA_ENTREVISTA").toString() : null)
                .estado((String) row.get("ESTADO"))
                .idEstado(row.get("ID_ESTADO") != null ? ((Number) row.get("ID_ESTADO")).intValue() : null)
                .etapa((String) row.get("ETAPA"))
                .idEtapa(row.get("ID_ETAPA") != null ? ((Number) row.get("ID_ETAPA")).intValue() : null)
                .build());
          }
        }

        // Construir la respuesta paginada
        var paginatedData = PaginatedResponse.<InterviewResponseDTO>builder()
            .items(list)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .currentPage(currentPage)
            .build();

        this.logger.info("Number of interviews fetched: {}, Total Elements in DB: {}", list.size(), totalElements);
        return new OperationResult<>(baseResponse, paginatedData);
      }

      return new OperationResult<>(baseResponse, null);

    } catch (Exception e) {
      this.logger.error("Error en listInterviews: ", e);
      return new OperationResult<>(new BaseResponse(3, "Error técnico: " + e.getMessage()), null);
    }
  }

  /**
   * Ejecuta el SP_ENTREVISTA_GET para obtener el detalle de una entrevista.
   * * @param idEntrevista ID de la entrevista
   * 
   * @param baseRequest Datos de auditoría y permisos
   * @return OperationResult con el detalle de la entrevista
   */
  public OperationResult<InterviewDetailResponseDTO> getInterviewById(Integer idEntrevista, BaseRequest baseRequest) {
    SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
        .withProcedureName("SP_ENTREVISTA_SEL");

    try {
      var params = new MapSqlParameterSource()
          .addValue("ID_ROL", baseRequest.getIdRol())
          .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
          .addValue("ID_USUARIO", baseRequest.getIdUsuario())
          .addValue("ID_ENTREVISTA", idEntrevista);

      this.logger.info("Getting interview detail {} for: {}", idEntrevista, baseRequest.getUsername());

      Map<String, Object> result = simpleJdbcCall.execute(params);

      // RS 1: Mensaje
      List<Map<String, Object>> rs1 = (List<Map<String, Object>>) result.get("#result-set-1");
      if (rs1 == null || rs1.isEmpty()) {
        return new OperationResult<>(new BaseResponse(3, "Sin respuesta del servidor"), null);
      }

      Map<String, Object> baseRow = rs1.get(0);
      Integer messageId = (Integer) baseRow.get("ID_TIPO_MENSAJE");
      BaseResponse baseResponse = new BaseResponse(messageId, (String) baseRow.get("MENSAJE"));

      if (messageId == 2) {
        // RS 2: Cabecera
        List<Map<String, Object>> rs2 = (List<Map<String, Object>>) result.get("#result-set-2");
        Map<String, Object> cabecera = rs2.get(0);

        // RS 3: RQs
        List<Map<String, Object>> rs3 = (List<Map<String, Object>>) result.get("#result-set-3");
        List<InterviewRqDTO> rqs = new ArrayList<>();
        List<String> uniqueClients = new ArrayList<>();

        if (rs3 != null) {
          for (Map<String, Object> row : rs3) {
            String c = (String) row.get("CLIENTE");
            rqs.add(InterviewRqDTO.builder()
                .id(row.get("ID") != null ? ((Number) row.get("ID")).intValue() : null)
                .label((String) row.get("LABEL"))
                .cliente(c)
                .build());

            if (c != null && !c.trim().isEmpty() && !uniqueClients.contains(c)) {
              uniqueClients.add(c);
            }
          }
        }

        // RS 4: Archivos
        List<InterviewFileDTO> files = new ArrayList<>();
        List<Map<String, Object>> rs4 = (List<Map<String, Object>>) result.get("#result-set-4");

        if (rs4 != null) {
          for (Map<String, Object> row : rs4) {
            files.add(InterviewFileDTO.builder()
                .id(row.get("ID") != null ? ((Number) row.get("ID")).intValue() : null)
                .name((String) row.get("NOMBRE_ARCHIVO"))
                .idFileType(
                    row.get("ID_TIPO_ARCHIVO") != null ? ((Number) row.get("ID_TIPO_ARCHIVO")).intValue() : null)
                .type((String) row.get("TIPO_ARCHIVO"))
                .build());
          }
        }

        // RS 5: Entrevistadores
        List<EntrevistadorDTO> entrevistadores = new ArrayList<>();
        List<GrabacionDTO> grabaciones = new ArrayList<>();
        List<Map<String, Object>> rs5 = (List<Map<String, Object>>) result.get("#result-set-5");
        List<Map<String, Object>> rs6 = (List<Map<String, Object>>) result.get("#result-set-6");

        if (rs5 != null) {
          for (Map<String, Object> row : rs5) {

            entrevistadores.add(new EntrevistadorDTO(
                (String) row.get("FULLNAME"),
                (String) row.get("EMAIL"),
                "1".equals(row.get("NOTIFICACION"))
            ));
          }
        }

        if (rs6 != null) {
          for (Map<String, Object> row : rs6) {
            grabaciones.add(new GrabacionDTO(
                (String) row.get("ENLACE"),
                (String) row.get("FECHA")
            ));
          }
        }

        // Mapeo Final
        var detail = InterviewDetailResponseDTO.builder()
            .id(cabecera.get("ID") != null ? ((Number) cabecera.get("ID")).intValue() : null)
            .idTalento(cabecera.get("ID_TALENTO") != null ? ((Number) cabecera.get("ID_TALENTO")).intValue() : null)
            .talento((String) cabecera.get("TALENTO"))
            .fecha((String) cabecera.get("FECHA"))
            .hora((String) cabecera.get("HORA"))
            .idEstado(cabecera.get("ID_ESTADO") != null ? ((Number) cabecera.get("ID_ESTADO")).intValue() : null)
            .estado((String) cabecera.get("ESTADO"))
            .idEtapa(cabecera.get("ID_ETAPA") != null ? ((Number) cabecera.get("ID_ETAPA")).intValue() : null)
            .etapa((String) cabecera.get("ETAPA"))
            .enlaceEntrevista((String) cabecera.get("ENLACE_ENTREVISTA"))
            .calificacion(cabecera.get("CALIFICACION") != null ? ((Integer) cabecera.get("CALIFICACION")).intValue() : 0)
            .calificacionPersonal(cabecera.get("CALIFICACION_PERSONAL") != null ? ((Integer) cabecera.get("CALIFICACION_PERSONAL")).intValue() : 0)
            .calificacionExperiencia(cabecera.get("CALIFICACION_EXPERIENCIA") != null ? ((Integer) cabecera.get("CALIFICACION_EXPERIENCIA")).intValue() : 0)
            .calificacionIdiomas(cabecera.get("CALIFICACION_IDIOMAS") != null ? ((Integer) cabecera.get("CALIFICACION_IDIOMAS")).intValue() : 0)
            .calificacionEducacion(cabecera.get("CALIFICACION_EDUCACION") != null ? ((Integer) cabecera.get("CALIFICACION_EDUCACION")).intValue() : 0)
            .notasPersonales((String) cabecera.get("NOTAS_PERSONALES"))
            .notasExperiencia((String) cabecera.get("NOTAS_EXPERIENCIA"))
            .notasIdiomas((String) cabecera.get("NOTAS_IDIOMAS"))
            .notasEducacion((String) cabecera.get("NOTAS_EDUCACION"))
            .motivoCancelacion((String) cabecera.get("MOTIVO_CANCELACION"))
            .perfil((String) cabecera.get("PUESTO"))
            .clienteResumen(String.join(", ", uniqueClients))
            .files(files)
            .selectedRQs(rqs)
            .entrevistadores(entrevistadores)
            .grabaciones(grabaciones)
            .build();

        return new OperationResult<>(baseResponse, detail);
      }

      return new OperationResult<>(baseResponse, null);

    } catch (Exception e) {
      this.logger.error("Error en getInterviewById: ", e);
      return new OperationResult<>(new BaseResponse(3, "Error técnico: " + e.getMessage()), null);
    }
  }

  /**
   * Ejecuta el SP_ENTREVISTA_UPD para actualizar una entrevista.
   */
  public OperationResult<Void> updateInterview(InterviewUpdateRequest request, BaseRequest baseRequest) {
    SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
        .withProcedureName("SP_ENTREVISTA_UPD");

    try {
      String lstIdReqJson = objectMapper.writeValueAsString(request.getIdsRqs());

      var params = new MapSqlParameterSource()
          .addValue("ID_ROL", baseRequest.getIdRol())
          .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
          .addValue("ID_USUARIO", baseRequest.getIdUsuario())
          .addValue("USUMOD", baseRequest.getUsername())
          .addValue("ID_ENTREVISTA", request.getIdEntrevista())
          .addValue("ID_TALENTO", request.getIdTalento())
          .addValue("FECHA", request.getFecha())
          .addValue("HORA", request.getHora())
          .addValue("ID_ESTADO", request.getEstado())
          .addValue("ID_ETAPA", request.getEtapa())
          .addValue("ENLACE_ENTREVISTA", request.getEnlaceEntrevista())
          .addValue("CALIFICACION", request.getCalificacion())
          .addValue("CALIFICACION_PERSONAL", request.getCalificacionPersonal())
          .addValue("CALIFICACION_EXPERIENCIA", request.getCalificacionExperiencia())
          .addValue("CALIFICACION_IDIOMAS", request.getCalificacionIdiomas())
          .addValue("CALIFICACION_EDUCACION", request.getCalificacionEducacion())
          .addValue("NOTAS_PERSONALES", request.getNotasPersonales())
          .addValue("NOTAS_EXPERIENCIA", request.getNotasExperiencia())
          .addValue("NOTAS_IDIOMAS", request.getNotasIdiomas())
          .addValue("NOTAS_EDUCACION", request.getNotasEducacion())
          .addValue("LST_ID_RQS", lstIdReqJson)
          .addValue("LST_ENTREVISTADORES", request.getEntrevistadores())
          .addValue("LST_GRABACIONES", request.getGrabaciones())
          .addValue("MOTIVO_CANCELACION", request.getMotivoCancelacion())
          .addValue("PERFIL", request.getPerfil());

      Map<String, Object> result = simpleJdbcCall.execute(params);
      List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

      if (resultSet == null || resultSet.isEmpty()) {
        return new OperationResult<>(new BaseResponse(3, "La base de datos no retornó información."), null);
      }

      Map<String, Object> row = resultSet.get(0);
      Integer messageId = (Integer) row.get("ID_TIPO_MENSAJE");
      String message = (String) row.get("MENSAJE");

      return new OperationResult<>(new BaseResponse(messageId, message), null);

    } catch (JsonProcessingException e) {
      this.logger.error("Error al serializar requerimientos a JSON", e);
      return new OperationResult<>(new BaseResponse(3, "Error de formato en requerimientos"), null);
    } catch (Exception e) {
      this.logger.error("Error crítico en updateInterview: ", e);
      return new OperationResult<>(new BaseResponse(3, "Error interno: " + e.getMessage()), null);
    }
  }

  /**
   * Ejecuta el SP_ENTREVISTAS_ARCHIVOS_INS para guardar la referencia del
   * archivo.
   */
  public OperationResult<Void> saveInterviewFile(
      Integer idEntrevista,
      Integer idTipoArchivo,
      String nombreArchivo,
      String rutaArchivo,
      BaseRequest baseRequest) {

    SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
        .withProcedureName("SP_ENTREVISTAS_ARCHIVOS_INS");

    try {
      var params = new MapSqlParameterSource()
          .addValue("ID_ENTREVISTA", idEntrevista)
          .addValue("ID_TIPO_ARCHIVO", idTipoArchivo)
          .addValue("NOMBRE_ARCHIVO", nombreArchivo)
          .addValue("RUTA_ARCHIVO", rutaArchivo)
          .addValue("USUCRE", baseRequest.getUsername())
          .addValue("ID_ROL", baseRequest.getIdRol())
          .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
          .addValue("ID_USUARIO", baseRequest.getIdUsuario());

      Map<String, Object> result = simpleJdbcCall.execute(params);
      List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

      if (resultSet == null || resultSet.isEmpty()) {
        this.logger.error("DB response is null or empty");
        return new OperationResult<>(new BaseResponse(3, "La base de datos no retornó información."), null);
      }

      Map<String, Object> row = resultSet.get(0);
      Integer messageId = (Integer) row.get("ID_TIPO_MENSAJE");
      String message = (String) row.get("MENSAJE");

      this.logger.info("DB response: {}", message);
      return new OperationResult<>(new BaseResponse(messageId, message), null);

    } catch (Exception e) {
      this.logger.error("Error saving file in DB: ", e);
      return new OperationResult<>(new BaseResponse(3, "Error interno: " + e.getMessage()), null);
    }
  }

  /**
   * Ejecuta el SP_ENTREVISTAS_ARCHIVOS_DEL para eliminar la referencia en BD.
   * Retorna la ruta del archivo en S3 para su posterior eliminación física.
   */
  public OperationResult<String> deleteInterviewFile(Integer idArchivo, BaseRequest baseRequest) {
    SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
        .withProcedureName("SP_ENTREVISTAS_ARCHIVOS_DEL");

    try {
      var params = new MapSqlParameterSource()
          .addValue("ID_ROL", baseRequest.getIdRol())
          .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
          .addValue("ID_USUARIO", baseRequest.getIdUsuario())
          .addValue("USUMOD", baseRequest.getUsername())
          .addValue("ID_ENTREVISTA_ARCHIVO", idArchivo);

      Map<String, Object> result = simpleJdbcCall.execute(params);
      List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

      if (resultSet == null || resultSet.isEmpty()) {
        return new OperationResult<>(new BaseResponse(3, "La base de datos no retornó información."), null);
      }

      Map<String, Object> row = resultSet.get(0);
      Integer messageId = (Integer) row.get("ID_TIPO_MENSAJE");
      String message = (String) row.get("MENSAJE");
      String rutaArchivo = (String) row.get("RUTA_ARCHIVO");

      return new OperationResult<>(new BaseResponse(messageId, message), rutaArchivo);

    } catch (Exception e) {
      this.logger.error("Error al eliminar archivo en BD: ", e);
      return new OperationResult<>(new BaseResponse(3, "Error técnico: " + e.getMessage()), null);
    }
  }
  
  //Buscar archivo por id

  public InterviewFileResponse getFileById(Integer idFile, BaseRequest baseRequest) {

    SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
        .withProcedureName("SP_ENTREVISTAS_ARCHIVO_SEL");

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("ID_ARCHIVO", idFile)
        .addValue("ID_ROL", baseRequest.getIdRol())
        .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
        .addValue("ID_USUARIO", baseRequest.getIdUsuario());

    Map<String, Object> result = jdbcCall.execute(params);

    List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("#result-set-1");

    List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-2");

    if (resultSet == null || resultSet.isEmpty()) {
      return null;
    }

    if (rows == null || rows.isEmpty()) {
        return null;
    }

    Map<String, Object> results = resultSet.get(0);

    InterviewFileResponse file = new InterviewFileResponse();
    file.setIdFile((Integer) results.get("idFile"));
    file.setFileName((String) results.get("nameFile"));
    file.setPathFile((String) results.get("pathFile"));

    return file;
  }

}