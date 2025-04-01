package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Repository
@RequiredArgsConstructor
public class HistoryRepository {
    private final JdbcTemplate jdbcTemplate;

    private Map<String, Object> executeProcedure(BaseRequest baseRequest, String SP, Consumer<MapSqlParameterSource> parameterBuilder) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName(SP);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("USERNAME", baseRequest.getUsername());
        parameterBuilder.accept(params);

        return simpleJdbcCall.execute(params);
    }

    public MovementReport registerMovement(BaseRequest baseRequest, EmployeeMovementRequest request) {
        LocalDate fchInicioContrato = Common.formatDate(request.getFchInicioContrato());
        LocalDate fchTerminoContrato = Common.formatDate(request.getFchTerminoContrato());

        String usernameUsuarioMovimiento = request.getNombres().charAt(0) + request.getApellidoPaterno().trim().toLowerCase();
        String correoUsuarioMovimiento = usernameUsuarioMovimiento + Constante.DOMINIO_CORREO;

        Map<String, Object> result = executeProcedure(baseRequest, "SP_USUARIOS_EMPLEADOS_UPD", params -> {
            params.addValue("ID_USUARIO_TALENTO", request.getIdUsuarioTalento())
                    .addValue("NOMBRES", request.getNombres())
                    .addValue("APELLIDO_PATERNO", request.getApellidoPaterno())
                    .addValue("APELLIDO_MATERNO", request.getApellidoMaterno())
                    .addValue("ID_UNIDAD", request.getIdArea())
                    .addValue("FCH_INICIO_CONTRATO", fchInicioContrato)
                    .addValue("FCH_TERMINO_CONTRATO", fchTerminoContrato)
                    .addValue("PROYECTO_SERVICIO", request.getProyectoServicio())
                    .addValue("OBJETO_CONTRATO", request.getObjetoContrato())
                    .addValue("ID_MONEDA", request.getIdMoneda())
                    .addValue("ID_MODALIDAD", request.getIdModalidad())
                    .addValue("EMPRESA", request.getIdCliente())
                    .addValue("MONTO_BASE", request.getMontoBase())
                    .addValue("MONTO_MOVILIDAD", request.getMontoMovilidad())
                    .addValue("MONTO_TRIMESTRAL", request.getMontoTrimestral())
                    .addValue("MONTO_SEMESTRAL", request.getMontoSemestral())
                    .addValue("PUESTO", request.getPuesto())
                    .addValue("AREA", request.getIdMovArea())
                    .addValue("JORNADA", request.getJornada())
                    .addValue("FCH_HISTORIAL", request.getFchMovimiento())
                    .addValue("USERNAME_EMPLEADO", usernameUsuarioMovimiento)
                    .addValue("EMAIL_EMPLEADO", correoUsuarioMovimiento);
        });

        List<Map<String, Object>> message = (List<Map<String, Object>>) result.get("#result-set-2");

        if (message != null && !message.isEmpty()) {
            Map<String, Object> row = message.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> report = (List<Map<String, Object>>) result.get("#result-set-3");
                Map<String, Object> reportRow = report.get(0);

                return mapToMovementReport(new BaseResponse(idTipoMensaje, mensaje), reportRow);
            }
        }
        return null;
    }

    private MovementReport mapToMovementReport(BaseResponse baseResponse, Map<String, Object> report) {
        return new MovementReport(
                baseResponse,
                (String) report.get("NOMBRES"),
                (String) report.get("APELLIDOS"),
                (String) report.get("UNIDAD"),
                (String) report.get("PUESTO"),
                (String) report.get("AREA"),
                (String) report.get("JORNADA"),
                (String) report.get("FCH_HISTORIAL"),
                (Double) report.get("MONTO_BASE"),
                (Double) report.get("MONTO_MOVILIDAD"),
                (Double) report.get("MONTO_TRIMESTRAL"),
                (String) report.get("CORREO_GESTOR"),
                (String) report.get("FIRMANTE"),
                (String) report.get("FIRMA"),
                (String) report.get("USERNAME_EMPLEADO"),
                (String) report.get("EMAIL_EMPLEADO")
        );
    }

    public CeseReport registerContractTermination(BaseRequest baseRequest, EmployeeContractEndRequest request) {
        String usernameUsuarioCese = request.getNombres().charAt(0) + request.getApellidoPaterno().trim().toLowerCase();
        String correoUsuarioCese = usernameUsuarioCese + Constante.DOMINIO_CORREO;

        Map<String, Object> result = executeProcedure(baseRequest, "SP_USUARIOS_EMPLEADOS_CESE", params -> {
            params.addValue("ID_USUARIO_TALENTO", request.getIdUsuarioTalento())
                    .addValue("NOMBRES", request.getNombres())
                    .addValue("APELLIDO_PATERNO", request.getApellidoPaterno())
                    .addValue("APELLIDO_MATERNO", request.getApellidoMaterno())
                    .addValue("ID_MOTIVO", request.getIdMotivo())
                    .addValue("EMPRESA", request.getIdCliente())
                    .addValue("ID_UNIDAD", request.getIdArea())
                    .addValue("FCH_HISTORIAL", request.getFchCese())
                    .addValue("USERNAME_EMPLEADO", usernameUsuarioCese)
                    .addValue("EMAIL_EMPLEADO", correoUsuarioCese);
        });

        List<Map<String, Object>> message = (List<Map<String, Object>>) result.get("#result-set-1");

        if (message != null && !message.isEmpty()) {
            Map<String, Object> row = message.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> report = (List<Map<String, Object>>) result.get("#result-set-2");
                Map<String, Object> reportRow = report.get(0);

                return mapToCeseReport(new BaseResponse(idTipoMensaje, mensaje), reportRow);
            }
        }
        return null;
    }

    private CeseReport mapToCeseReport(BaseResponse baseResponse, Map<String, Object> report) {
        return new CeseReport(
                baseResponse,
                (String) report.get("NOMBRES"),
                (String) report.get("APELLIDOS"),
                (String) report.get("UNIDAD"),
                (String) report.get("MOTIVO"),
                (String) report.get("FCH_HISTORIAL"),
                (String) report.get("CORREO_GESTOR"),
                (String) report.get("FIRMANTE"),
                (String) report.get("FIRMA"),
                (String) report.get("USERNAME_EMPLEADO"),
                (String) report.get("EMAIL_EMPLEADO")
        );
    }

    public EntryReport registerEntry(BaseRequest baseRequest, EmployeeEntryRequest request) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_USUARIOS_EMPLEADOS_INS");

        LocalDate fchInicioContrato = Common.formatDate(request.getFchInicioContrato());
        LocalDate fchTerminoContrato = Common.formatDate(request.getFchTerminoContrato());

        String usernameUsuarioIngreso = request.getNombres().charAt(0) + request.getApellidoPaterno().trim();
        String correoUsuarioIngreso = usernameUsuarioIngreso.toLowerCase() + Constante.DOMINIO_CORREO;

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_TALENTO", request.getIdTalento())
                .addValue("NOMBRES", request.getNombres())
                .addValue("APELLIDO_PATERNO", request.getApellidoPaterno())
                .addValue("APELLIDO_MATERNO", request.getApellidoMaterno())
                // USUARIO EMPLEADO
                .addValue("ID_USUARIO_TALENTO", request.getIdUsuarioTalento())
                .addValue("ID_AREA", request.getIdArea())
                .addValue("CARGO", request.getCargo())
                .addValue("ID_CLIENTE", request.getIdCliente())
                .addValue("HORARIO_TRABAJO", request.getHorarioTrabajo())
                .addValue("FCH_INICIO_CONTRATO", fchInicioContrato)
                .addValue("FCH_TERMINO_CONTRATO", fchTerminoContrato)
                .addValue("PROYECTO_SERVICIO", request.getProyectoServicio())
                .addValue("OBJETO_CONTRATO", request.getObjetoContrato())
                // HISTORIAL
                .addValue("ID_MOTIVO", request.getIdMotivo())
                .addValue("ID_MONEDA", request.getIdMoneda())
                .addValue("ID_MODALIDAD", request.getIdModalidad())
                .addValue("CLIENTE", request.getCliente())
                .addValue("DECLARAR_SUNAT", request.getDeclararSunat())
                .addValue("SEDE_DECLARAR", request.getSedeDeclarar())
                .addValue("MONTO_BASE", request.getMontoBase())
                .addValue("MONTO_MOVILIDAD", request.getMontoMovilidad())
                .addValue("MONTO_TRIMESTRAL", request.getMontoTrimestral())
                .addValue("MONTO_SEMESTRAL", request.getMontoSemestral())
                .addValue("FCH_HISTORIAL", request.getFchHistorial())
                .addValue("USERNAME_EMPLEADO", usernameUsuarioIngreso)
                .addValue("EMAIL_EMPLEADO", correoUsuarioIngreso)
                // VALIDAR ROL
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("USUARIO", baseRequest.getUsername())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> message = (List<Map<String, Object>>) result.get("#result-set-2");

        if (message != null && !message.isEmpty()) {
            Map<String, Object> row = message.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> report = (List<Map<String, Object>>) result.get("#result-set-3");
                Map<String, Object> reportRow = report.get(0);

                return mapToEntryReport(new BaseResponse(idTipoMensaje, mensaje), reportRow);
            }
        }
        return null;
    }

    private EntryReport mapToEntryReport(BaseResponse response, Map<String, Object> report) {
        return new EntryReport(
                response,
                (String) report.get("NOMBRES"),
                (String) report.get("APELLIDOS"),
                (String) report.get("UNIDAD"),
                (String) report.get("FCH_HISTORIAL"),
                (String) report.get("MODALIDAD"),
                (String) report.get("MOTIVO"),
                (String) report.get("CARGO"),
                (String) report.get("JORNADA"),
                (Double) report.get("MONTO_BASE"),
                (Double) report.get("MONTO_MOVILIDAD"),
                (Double) report.get("MONTO_TRIMESTRAL"),
                (String) report.get("FCH_INICIO_CONTRATO"),
                (String) report.get("FCH_TERMINO_CONTRATO"),
                (String) report.get("PROYECTO_SERVICIO"),
                (String) report.get("OBJETO_CONTRATO"),
                (Integer) report.get("DECLARAR_SUNAT"),
                (String) report.get("SEDE_DECLARAR"),
                (String) report.get("CORREO_GESTOR"),
                (String) report.get("FIRMANTE"),
                (String) report.get("FIRMA"),
                (String) report.get("USERNAME_EMPLEADO"),
                (String) report.get("EMAIL_EMPLEADO")
        );
    }

    public Object getLastEmployeeHistoryRegister(BaseRequest baseRequest, Integer idTipoHistorial, Integer idUsuarioTalento) {
        Object report = null;
        Map<String, Object> result = executeProcedure(baseRequest, "SP_HISTORIAL_SEL", params -> {
            params.addValue("ID_TIPO_HISTORIAL", idTipoHistorial)
                    .addValue("ID_USUARIO_TALENTO", idUsuarioTalento);
        });

        List<Map<String, Object>> message = (List<Map<String, Object>>) result.get("#result-set-1");

        if (message != null && !message.isEmpty()) {
            Map<String, Object> row = message.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> reportData = (List<Map<String, Object>>) result.get("#result-set-2");
                Map<String, Object> reportRow = reportData.get(0);

                switch (idTipoHistorial) {
                    case 1:
                        report = mapToEntryReport(new BaseResponse(idTipoMensaje, mensaje), reportRow);
                        break;
                    case 2:
                        report = mapToMovementReport(new BaseResponse(idTipoMensaje, mensaje), reportRow);
                        break;
                    case 3:
                        report = mapToCeseReport(new BaseResponse(idTipoMensaje, mensaje), reportRow);
                        break;
                }
                return report;
            }
        }
        return null;
    }
}
