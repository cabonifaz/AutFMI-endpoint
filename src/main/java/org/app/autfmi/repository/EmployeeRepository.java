package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.response.BaseResponse;
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

    public BaseResponse saveEmployeeEntry(BaseRequest baseRequest, EmployeeEntryRequest request) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_USUARIOS_EMPLEADOS_INS");

        LocalDate fchInicioContrato = Common.formatDate(request.getFchInicioContrato());
        LocalDate fchTerminoContrato = Common.formatDate(request.getFchTerminoContrato());

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_TALENTO", request.getIdTalento())
                .addValue("NOMBRES", request.getNombres())
                .addValue("APELLIDOS", request.getApellidos())
                .addValue("ID_USUARIO_TALENTO", request.getIdUsuarioTalento())
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("ID_UNIDAD", request.getIdUnidad())
                .addValue("CARGO", request.getCargo())
                .addValue("FCH_INICIO_CONTRATO", fchInicioContrato)
                .addValue("FCH_TERMINO_CONTRATO", fchTerminoContrato)
                .addValue("PROYECTO_SERVICIO", request.getProyectoServicio())
                .addValue("OBJETO_CONTRATO", request.getObjetoContrato())
                // HISTORIAL
                .addValue("ID_MODALIDAD", request.getIdModalidad())
                .addValue("ID_MOTIVO", request.getIdMotivo())
                .addValue("ID_MONEDA", request.getIdMoneda())
                .addValue("EMPRESA", request.getEmpresa())
                .addValue("DECLARAR_SUNAT", request.getDeclararSunat())
                .addValue("SEDE_DECLARAR", request.getSedeDeclarar())
                .addValue("MONTO_BASE", request.getMontoBase())
                .addValue("MONTO_MOVILIDAD", request.getMontoMovilidad())
                .addValue("MONTO_TRIMESTRAL", request.getMontoTrimestral())
                .addValue("MONTO_SEMESTRAL", request.getMontoSemestral())
                .addValue("FCH_HISTORIAL", request.getFchHistorial())
                // VALIDAR ROL
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("USERNAME", baseRequest.getUsername());

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            return new BaseResponse(
                    (Integer) row.get("ID_TIPO_MENSAJE"),
                    (String) row.get("MENSAJE")
            );
        }
        return null;
    }
}
