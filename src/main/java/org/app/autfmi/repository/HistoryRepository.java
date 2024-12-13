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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Repository
@RequiredArgsConstructor
public class HistoryRepository {
    private final JdbcTemplate jdbcTemplate;

    private BaseResponse executeProcedure(BaseRequest baseRequest, Consumer<MapSqlParameterSource> parameterBuilder) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_HISTORIAL_INS");

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ID_USUARIO", baseRequest.getIdUsuario());
        parameterBuilder.accept(params);

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            return new BaseResponse((Integer) row.get("ID_TIPO_MENSAJE"), (String) row.get("MENSAJE"));
        }
        return null;
    }

    public BaseResponse registerEntry(BaseRequest baseRequest, EmployeeEntryRequest request) {
        return executeProcedure(baseRequest, params -> {
            params.addValue("ID_TIPO_HISTORIAL", Constante.HISTORIAL_INGRESO)
                    .addValue("ID_USUARIO_TALENTO", request.getIdUsuarioTalento())
                    .addValue("MOTIVO", request.getMotivo())
                    .addValue("EMPRESA", request.getEmpresa())
                    .addValue("UNIDAD", request.getUnidad())
                    .addValue("MONTO_BASE", request.getMontoBase())
                    .addValue("MONTO_MOVILIDAD", request.getMontoMovilidad())
                    .addValue("MONTO_TRIMESTRAL", request.getMontoTrimestral())
                    .addValue("MONTO_SEMESTRAL", request.getMontoSemestral())
                    .addValue("PUESTO", null)
                    .addValue("AREA", null)
                    .addValue("JORNADA", null)
                    .addValue("DECLARAR_SUNAT", request.getDeclararSunat())
                    .addValue("SEDE_DECLARAR", request.getSedeDeclarar())
                    .addValue("FCH_HISTORIAL", request.getFchHistorial());
        });
    }

    public BaseResponse registerMovement(BaseRequest baseRequest, EmployeeMovementRequest request) {
        return executeProcedure(baseRequest, params -> {
            params.addValue("ID_TIPO_HISTORIAL", Constante.HISTORIAL_MOVIMIENTO)
                    .addValue("ID_USUARIO_TALENTO", request.getIdUsuarioTalento())
                    .addValue("MOTIVO", null)
                    .addValue("EMPRESA", request.getEmpresa())
                    .addValue("UNIDAD", request.getUnidad())
                    .addValue("MONTO_BASE", request.getMontoBase())
                    .addValue("MONTO_MOVILIDAD", request.getMontoMovilidad())
                    .addValue("MONTO_TRIMESTRAL", request.getMontoTrimestral())
                    .addValue("MONTO_SEMESTRAL", request.getMontoSemestral())
                    .addValue("PUESTO", request.getPuesto())
                    .addValue("AREA", request.getArea())
                    .addValue("JORNADA", request.getJornada())
                    .addValue("DECLARAR_SUNAT", null)
                    .addValue("SEDE_DECLARAR", null)
                    .addValue("FCH_HISTORIAL", request.getFchMovimiento());
        });
    }

    public BaseResponse registerContractTermination(BaseRequest baseRequest, EmployeeContractEndRequest request) {
        return executeProcedure(baseRequest, params -> {
            params.addValue("ID_TIPO_HISTORIAL", Constante.HISTORIAL_CESE)
                    .addValue("ID_USUARIO_TALENTO", request.getIdUsuarioTalento())
                    .addValue("MOTIVO", request.getMotivoCese())
                    .addValue("EMPRESA", request.getEmpresa())
                    .addValue("UNIDAD", request.getUnidad())
                    .addValue("MONTO_BASE", null)
                    .addValue("MONTO_MOVILIDAD", null)
                    .addValue("MONTO_TRIMESTRAL", null)
                    .addValue("MONTO_SEMESTRAL", null)
                    .addValue("PUESTO", null)
                    .addValue("AREA", null) 
                    .addValue("JORNADA", null)
                    .addValue("DECLARAR_SUNAT", null)
                    .addValue("SEDE_DECLARAR", null)
                    .addValue("FCH_HISTORIAL", request.getFchCese());
        });
    }
}
