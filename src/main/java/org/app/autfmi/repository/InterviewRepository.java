package org.app.autfmi.repository;

import java.util.List;
import java.util.Map;

import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.InterviewRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.OperationResult;
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
  private final Logger log = LoggerFactory.getLogger(InterviewRepository.class);

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
      this.log.error("Error al serializar requerimientos a JSON", e);
      return new OperationResult<>(new BaseResponse(3, "Error de formato en requerimientos"), null);
    } catch (Exception e) {
      this.log.error("Error crítico en createInterview: ", e);
      return new OperationResult<>(new BaseResponse(3, "Error interno: " + e.getMessage()), null);
    }
  }
}
