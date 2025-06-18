package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.response.BaseResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AuthRepository {
    private final JdbcTemplate jdbcTemplate;

    public BaseResponse verifyCredentials(String username, String password) {

        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_VERIFY_CREDENTIALS");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("USUARIO", username)
                .addValue("CLAVE", password)
                .addValue("ID_MODULO", 1);

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            return new BaseResponse(idTipoMensaje, mensaje);
        } else {
            return null;
        }
    }

    public Map<String, Object> getUserByUsername(String username) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_USUARIOS_SEL");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("USUARIO", username);

        return simpleJdbcCall.execute(params);
    }

    public UserDTO mapToUserDTO(Map<String, Object> data) {
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) data.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");

            if (idTipoMensaje == 2) {
                return  processUserData(data);
            }
        }
        return null;
    }

    private UserDTO processUserData(Map<String, Object> data) {
        List<Map<String, Object>> resultSet2 = (List<Map<String, Object>>) data.get("#result-set-2");

        if (resultSet2 != null && !resultSet2.isEmpty()) {
            Map<String, Object> userData = resultSet2.get(0);

            // Extract the roles from #result-set-3
            List<Map<String, Object>> resultSet3 = (List<Map<String, Object>>) data.get("#result-set-3");

            List<Integer> idRoles = new ArrayList<>();
            List<String> nameRoles = new ArrayList<>();
            if (resultSet3 != null && !resultSet3.isEmpty()) {
                for (Map<String, Object> roleData : resultSet3) {
                    // El campo STRING1 contiene los roles
                    idRoles.add((Integer) roleData.get("ID_ROL"));
                    nameRoles.add((String) roleData.get("ROL"));
                }
            }

            return new UserDTO(
                    (Integer) userData.get("ID_USUARIO"),   // From result-set-2
                    (Integer) userData.get("ID_EMPRESA"),
                    (String) userData.get("USUARIO"),
                    (String) userData.get("NOMBRES"),
                    (String) userData.get("APELLIDOS"),
                    idRoles,
                    nameRoles
            );
        }
        return null;
    }
}
