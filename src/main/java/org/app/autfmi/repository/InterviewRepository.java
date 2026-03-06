package org.app.autfmi.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.InterviewResponseDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.InterviewListRequest;
import org.app.autfmi.model.request.InterviewRequest;
import org.app.autfmi.model.response.BaseResponse;
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
          .addValue("USUARIO", baseRequest.getUsername())
          .addValue("ID_USUARIO", baseRequest.getIdUsuario())
          .addValue("ID_ROL", baseRequest.getIdRol())
          .addValue("FUNCIONALIDADES", baseRequest.getFuncionalidades());

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
}
