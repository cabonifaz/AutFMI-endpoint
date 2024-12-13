package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.util.Constante;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Repository
@RequiredArgsConstructor
public class HistoryRepository {
    private final JdbcTemplate jdbcTemplate;

    private BaseResponse executeProcedure(BaseRequest baseRequest, String SP, Consumer<MapSqlParameterSource> parameterBuilder) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName(SP);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("USERNAME", baseRequest.getUsername());
        parameterBuilder.accept(params);

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            return new BaseResponse((Integer) row.get("ID_TIPO_MENSAJE"), (String) row.get("MENSAJE"));
        }
        return null;
    }

    public BaseResponse registerMovement(BaseRequest baseRequest, EmployeeMovementRequest request) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate fchInicioContrato = LocalDate.parse(request.getFchInicioContrato(), formatter);
        LocalDate fchTerminoContrato = LocalDate.parse(request.getFchTerminoContrato(), formatter);

        return executeProcedure(baseRequest,"SP_USUARIOS_EMPLEADOS_UPD", params -> {
            params.addValue("ID_USUARIO_TALENTO", request.getIdUsuarioTalento())
                    .addValue("ID_UNIDAD", request.getIdUnidad())
                    .addValue("CARGO", request.getCargo())
                    .addValue("FCH_INICIO_CONTRATO", fchInicioContrato)
                    .addValue("FCH_TERMINO_CONTRATO", fchTerminoContrato)
                    .addValue("PROYECTO_SERVICIO", request.getProyectoServicio())
                    .addValue("OBJETO_CONTRATO", request.getObjetoContrato())
                    .addValue("ID_MONEDA", request.getIdMoneda())
                    .addValue("ID_MODALIDAD", request.getIdModalidad())
                    .addValue("EMPRESA", request.getEmpresa())
                    .addValue("MONTO_BASE", request.getMontoBase())
                    .addValue("MONTO_MOVILIDAD", request.getMontoMovilidad())
                    .addValue("MONTO_TRIMESTRAL", request.getMontoTrimestral())
                    .addValue("MONTO_SEMESTRAL", request.getMontoSemestral())
                    .addValue("PUESTO", request.getPuesto())
                    .addValue("AREA", request.getArea())
                    .addValue("JORNADA", request.getJornada())
                    .addValue("FCH_HISTORIAL", request.getFchMovimiento());
        });
    }

    public BaseResponse registerContractTermination(BaseRequest baseRequest, EmployeeContractEndRequest request) {
        return executeProcedure(baseRequest, "SP_USUARIOS_EMPLEADOS_CESE", params -> {
            params.addValue("ID_USUARIO_TALENTO", request.getIdUsuarioTalento())
                    .addValue("ID_MOTIVO", request.getIdMotivo())
                    .addValue("EMPRESA", request.getEmpresa())
                    .addValue("ID_UNIDAD", request.getIdUnidad())
                    .addValue("FCH_HISTORIAL", request.getFchCese());
        });
    }
}
