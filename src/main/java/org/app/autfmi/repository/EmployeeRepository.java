package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.EmployeeEntryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class EmployeeRepository {
    private final JdbcTemplate jdbcTemplate;

    public EmployeeEntryResponse saveEmployeeEntry(BaseRequest baseRequest, EmployeeEntryRequest request) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_USUARIOS_EMPLEADOS_INS");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate fchInicioContrato = LocalDate.parse(request.getFchInicioContrato(), formatter);
        LocalDate fchTerminoContrato = LocalDate.parse(request.getFchTerminoContrato(), formatter);

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_TALENTO", request.getIdTalento())
                .addValue("NOMBRES", request.getNombres())
                .addValue("APELLIDOS", request.getApellidos())
                .addValue("ID_USUARIO_TALENTO", request.getIdUsuarioTalento())
                .addValue("ID_EMPRESA", request.getIdEmpresa())
                .addValue("ID_UNIDAD", request.getIdUnidad())
                .addValue("CARGO", request.getCargo())
                .addValue("FCH_INICIO_CONTRATO", fchInicioContrato)
                .addValue("FCH_TERMINO_CONTRATO", fchTerminoContrato)
                .addValue("PROYECTO_SERVICIO", request.getProyectoServicio())
                .addValue("OBJETO_CONTRATO", request.getObjetoContrato())
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("ID_USUARIO", baseRequest.getIdUsuario());

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            return new EmployeeEntryResponse(
                    (Integer) row.get("ID_TIPO_MENSAJE"),
                    (String) row.get("MENSAJE"),
                    (Integer) row.get("ID_USUARIO_TALENTO")
            );
        }
        return null;
    }
}
