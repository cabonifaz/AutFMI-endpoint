package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.RequirementItemDTO;
import org.app.autfmi.model.dto.TarifarioDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.RequirementListResponse;
import org.app.autfmi.model.response.TarifarioListResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class TarifarioRepository {
    private final JdbcTemplate jdbcTemplate;

    public BaseResponse listaTarifario(BaseRequest baseRequest) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_TARIFARIO_LST");

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
                List<Map<String, Object>> tarifarioSet = (List<Map<String, Object>>) result.get("#result-set-2");
                List<TarifarioDTO> tarifarioList = new ArrayList<>();
                if (tarifarioSet != null && !tarifarioSet.isEmpty()) {
                    for (Map<String, Object> tarifaRow : tarifarioSet) {
                        tarifarioList.add(mapToTarifarioItemDTO(tarifaRow));
                    }
                }
                return new TarifarioListResponse(idTipoMensaje, mensaje, tarifarioList);
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }


    protected static TarifarioDTO mapToTarifarioItemDTO(Map<String, Object> tarifa) {
        return new TarifarioDTO(
                (Integer) tarifa.get("ID_PERFIL"),
                (String) tarifa.get("PERFIL"),
                (BigDecimal) tarifa.get("TARIFA")
        );
    }


}
