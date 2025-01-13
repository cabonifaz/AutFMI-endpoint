package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.EmployeeDTO;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.EmployeeResponse;
import org.app.autfmi.util.Common;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class EmployeeRepository {
    private final JdbcTemplate jdbcTemplate;

    public BaseResponse getEmployee(Integer idUsuarioTalento) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_USUARIOS_EMPLEADOS_SEL");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_USUARIO_TALENTO", idUsuarioTalento);

        Map<String, Object> result = simpleJdbcCall.execute(params);

        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> resultSet2 = (List<Map<String, Object>>) result.get("#result-set-2");

                if (resultSet2 != null && !resultSet2.isEmpty()) {
                    Map<String, Object> employeeRaw = resultSet2.get(0);
                    return new EmployeeResponse( idTipoMensaje, mensaje, mapToEmployeeDTO(employeeRaw) );
                }
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    private EmployeeDTO mapToEmployeeDTO(Map<String, Object> employeeRaw) {
        return new EmployeeDTO(
                (String) employeeRaw.get("NOMBRES"),
                (String) employeeRaw.get("APELLIDOS"),
                (Integer) employeeRaw.get("ID_UNIDAD"),
                (Double) employeeRaw.get("REMUNERACION")
        );
    }
}
