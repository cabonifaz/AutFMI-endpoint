package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.ClientItemDTO;
import org.app.autfmi.model.dto.RequirementItemDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.ClientListResponse;
import org.app.autfmi.model.response.RequirementListResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ClientRepository {
    private final JdbcTemplate jdbcTemplate;

    public BaseResponse listClients(BaseRequest baseRequest){
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_CLIENTE_LST");

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("USUARIO", baseRequest.getUsername())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> clientSet = (List<Map<String, Object>>) result.get("#result-set-2");
                List<ClientItemDTO> clientList = new ArrayList<>();
                if (clientSet != null && !clientSet.isEmpty()) {
                    for (Map<String, Object> clientRow : clientSet) {
                        clientList.add(mapClientItemDTO(clientRow));
                    }
                }
                return new ClientListResponse(idTipoMensaje, mensaje, clientList);
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    private ClientItemDTO mapClientItemDTO(Map<String, Object> client) {
        return new ClientItemDTO(
                (Integer) client.get("ID_CLIENTE"),
                (String) client.get("RAZON_SOCIAL"),
                (Integer) client.get("TOTAL")
        );
    }

}
