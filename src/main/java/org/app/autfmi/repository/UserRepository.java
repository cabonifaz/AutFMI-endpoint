package org.app.autfmi.repository;

import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.OperationResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unchecked")
@Repository
public class UserRepository {

  private final JdbcTemplate jdbcTemplate;

  public UserRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public OperationResult<UserDTO> uploadSignature(Integer userId, String signaturePath, BaseRequest baseRequest) {

    var operationResult = new OperationResult<UserDTO>();

    if (this.jdbcTemplate == null) {
      throw new IllegalArgumentException("JdbcTemplate is null");
    }

    var simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
        .withProcedureName("SP_USUARIO_ACTUALIZAR_FIRMA");

    var params = new MapSqlParameterSource()
        .addValue("ID_USUARIO_OBJETIVO", userId)
        .addValue("ID_USUARIO", baseRequest.getIdUsuario())
        .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
        .addValue("ID_ROL", baseRequest.getIdRol())
        .addValue("USUARIO", baseRequest.getUsername())
        .addValue("FIRMA", signaturePath)
        .addValue("FUNCIONALIDADES", baseRequest.getFuncionalidades());

    var result = simpleJdbcCall.execute(params);
    var baseResult = (List<Map<String, Object>>) result.get("#result-set-1");

    if (baseResult == null || baseResult.isEmpty()) {
      operationResult.setBaseResponse(new BaseResponse(3, "Error al subir la firma"));
      return operationResult;
    }

    var baseResponseDb = (Map<String, Object>) baseResult.get(0);
    var messageId = (Integer) baseResponseDb.getOrDefault("ID_TIPO_MENSAJE", 3);
    var message = (String) baseResponseDb.getOrDefault("MENSAJE", "Error al actualizar la firma en base de datos");

    var baseResponse = new BaseResponse(messageId, message);
    operationResult.setBaseResponse(baseResponse);

    if (messageId != 2) {
      return operationResult;
    }

    // Mapear usuario
    var resultSetUser = (List<Map<String, Object>>) result.get("#result-set-2");
    var userData = resultSetUser.get(0);

    var userBuilder = new UserDTO();

    userBuilder.setIdUsuario((Integer) userData.get("ID"));
    userBuilder.setUsuario((String) userData.get("USUARIO"));
    userBuilder.setNombres((String) userData.get("NOMBRES"));
    userBuilder.setApellidos((String) userData.get("APELLIDOS"));

    operationResult.setBaseResponse(new BaseResponse(messageId, message));
    operationResult.setData(userBuilder);

    return operationResult;
  }

}
