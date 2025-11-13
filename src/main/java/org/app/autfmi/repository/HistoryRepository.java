package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.report.*;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Repository
@RequiredArgsConstructor
public class HistoryRepository {
    @NonNull
    private final JdbcTemplate jdbcTemplate;
    private final Logger logger = LoggerFactory.getLogger(HistoryRepository.class);

    private Map<String, Object> executeProcedure(BaseRequest baseRequest, @NonNull String SP,
            Consumer<MapSqlParameterSource> parameterBuilder) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName(SP);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("USUARIO", baseRequest.getUsername())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());
        parameterBuilder.accept(params);

        return simpleJdbcCall.execute(params);
    }

    /**
     * Register employee movement
     * 
     * @param baseRequest
     * @param request
     * @return MovementReport
     */
    public MovementReport registerMovement(BaseRequest baseRequest, EmployeeMovementRequest request) {
        LocalDate fchInicioContrato = Common.formatDate(request.getFchInicioContrato());
        LocalDate fchTerminoContrato = Common.formatDate(request.getFchTerminoContrato());

        Map<String, Object> result = executeProcedure(baseRequest, "SP_TALENTO_EMPLEADO_MOVIMIENTO", params -> params
                .addValue("ID_TALENTO", request.getIdTalento())
                .addValue("NOMBRES", request.getNombres())
                .addValue("APELLIDO_PATERNO", request.getApellidoPaterno())
                .addValue("APELLIDO_MATERNO", request.getApellidoMaterno())

                .addValue("ID_AREA", request.getIdArea())
                .addValue("AREA", request.getArea())
                .addValue("FCH_INICIO_CONTRATO", fchInicioContrato)
                .addValue("FCH_TERMINO_CONTRATO", fchTerminoContrato)
                .addValue("PROYECTO_SERVICIO", request.getProyectoServicio())
                .addValue("OBJETO_CONTRATO", request.getObjetoContrato())

                .addValue("ID_MODALIDAD", request.getIdModalidad())
                .addValue("ID_CLIENTE", request.getIdCliente())
                .addValue("CLIENTE", request.getCliente())
                .addValue("MONTO_BASE", request.getMontoBase())
                .addValue("MONTO_MOVILIDAD", request.getMontoMovilidad())
                .addValue("MONTO_TRIMESTRAL", request.getMontoTrimestral())
                .addValue("MONTO_SEMESTRAL", request.getMontoSemestral())
                .addValue("ID_MONEDA", request.getIdMoneda())
                .addValue("PUESTO", request.getPuesto())
                .addValue("ID_MOV_AREA", request.getIdMovArea())
                .addValue("MOV_AREA", request.getMovArea())
                .addValue("HORARIO", request.getHorario())
                .addValue("FCH_HISTORIAL", request.getFchMovimiento()));

        List<Map<String, Object>> message = (List<Map<String, Object>>) result.get("#result-set-1");

        if (message != null && !message.isEmpty()) {
            Map<String, Object> row = message.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> report = (List<Map<String, Object>>) result.get("#result-set-2");
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
                (String) report.get("AREA"),
                (String) report.get("PUESTO"),
                (String) report.get("MOV_AREA"),
                (String) report.get("HORARIO"),
                (String) report.get("FCH_HISTORIAL"),
                (String) report.get("MONTO_BASE"),
                (String) report.get("MONTO_MOVILIDAD"),
                (String) report.get("MONTO_TRIMESTRAL"),
                (String) report.get("CORREO_GESTOR"),
                (String) report.get("FIRMANTE"),
                (String) report.get("FIRMA"),
                (String) report.get("USERNAME_EMPLEADO"),
                (String) report.get("EMAIL_EMPLEADO"));
    }

    /**
     * Register contract termination
     * 
     * @param baseRequest
     * @param request
     * @return OperationResult<Integer> contiene el ID de la operación para futuras
     *         operaciones
     */
    public OperationResult<Integer> registerContractTermination(BaseRequest baseRequest,
            EmployeeContractEndRequest request) {
        Map<String, Object> result = executeProcedure(baseRequest, "SP_TALENTO_EMPLEADO_CESE", params -> {
            params.addValue("ID_TALENTO", request.getIdTalento())
                    .addValue("NOMBRES", request.getNombres())
                    .addValue("APELLIDO_PATERNO", request.getApellidoPaterno())
                    .addValue("APELLIDO_MATERNO", request.getApellidoMaterno())
                    .addValue("ID_MOTIVO", request.getIdMotivo())
                    .addValue("ID_CLIENTE", request.getIdCliente())
                    .addValue("CLIENTE", request.getCliente())
                    .addValue("ID_AREA", request.getIdArea())
                    .addValue("FCH_HISTORIAL", request.getFchCese());
        });

        List<Map<String, Object>> dbResponse = (List<Map<String, Object>>) result.get("#result-set-1");

        if (dbResponse == null || dbResponse.isEmpty()) {
            this.logger.error("No message returned from stored procedure SP_TALENTO_EMPLEADO_CESE");
            this.logger.error("Error: {}", result);
            BaseResponse baseResponse = new BaseResponse(3, "Error al realizar la consulta",
                    "No se obtuvo respuesta de la base de datos");
            return new OperationResult<>(baseResponse, null);
        }

        Integer messageId = (Integer) dbResponse.get(0).get("ID_TIPO_MENSAJE");
        String messageText = (String) dbResponse.get(0).get("MENSAJE");
        Integer operationId = (Integer) dbResponse.get(0).get("ID_OPERACION");

        BaseResponse baseResponse = new BaseResponse(messageId, messageText);
        return new OperationResult<>(baseResponse, operationId);
    }

    public IReport getHistoryReport(
            BaseRequest baseRequest,
            @NonNull Integer talentId,
            @NonNull Integer reportTypeRequest,
            @Nullable Integer operationId,
            @NonNull Boolean isLast) {

        this.logger.info("Fetching employee history register for TipoHistorial: {} and Talento: {} OperationId: {}",
                reportTypeRequest, talentId, operationId);
        Map<String, Object> rs = executeProcedure(baseRequest, "SP_FMI_REPORTE_GENERAL", params -> {
            params
                    .addValue("ID_TALENTO", talentId)
                    .addValue("ID_TIPO_REPORTE", reportTypeRequest)
                    .addValue("ID_OPERACION", operationId)
                    .addValue("ULTIMO", isLast);
        });

        if (rs == null || rs.isEmpty()) {
            this.logger.error("No message returned from stored procedure SP_FMI_REPORTE_GENERAL");
            this.logger.error("Error: {}", rs);
            return new BaseReport(new BaseResponse(3, "Error al realizar la consulta"));
        }

        List<Map<String, Object>> messageList = (List<Map<String, Object>>) rs.get("#result-set-1");

        if (messageList == null || messageList.isEmpty()) {
            this.logger.error("No message returned from stored procedure SP_FMI_REPORTE_GENERAL");
            return new BaseReport(new BaseResponse(3, "Error al realizar la consulta"));
        }

        Map<String, Object> baseResponseDb = messageList.get(0);
        Integer messageId = (Integer) baseResponseDb.get("ID_TIPO_MENSAJE");
        String messageText = (String) baseResponseDb.get("MENSAJE");
        String reportType = (String) baseResponseDb.get("TIPO_REPORTE");
        BaseResponse baseResponse = new BaseResponse(messageId, messageText);

        if (baseResponse.getIdTipoMensaje() != 2)
            return new BaseReport(baseResponse);

        // Obtener datos del reporte
        List<Map<String, Object>> reportData = (List<Map<String, Object>>) rs.get("#result-set-2");

        if (reportData == null || reportData.isEmpty()) {
            String message = "El talento no tiene reporte de " + reportType + " previo";
            return new BaseReport(new BaseResponse(1, message));
        }

        switch (reportTypeRequest) {
            case Constante.TIPO_REPORTE_INGRESO:
                return mapToEntryReport(baseResponse, reportData.get(0));

            case Constante.TIPO_REPORTE_MOVIMIENTO:
                return mapToMovementReport(baseResponse, reportData.get(0));

            case Constante.TIPO_REPORTE_CESE:
                return mapToCeseReport(baseResponse, reportData.get(0));

            default:
                this.logger.error("Tipo de reporte no manejado: {}", reportTypeRequest);
                return new BaseReport(new BaseResponse(3, "Tipo de reporte no soportado"));
        }
    }

    private CeseReport mapToCeseReport(BaseResponse baseResponse, Map<String, Object> report) {
        return new CeseReport(
                baseResponse,
                (String) report.get("NOMBRES"),
                (String) report.get("APELLIDOS"),
                (String) report.get("AREA"),
                (String) report.get("MOTIVO"),
                (String) report.get("FCH_HISTORIAL"),
                (String) report.get("CORREO_GESTOR"),
                (String) report.get("FIRMANTE"),
                (String) report.get("FIRMA"),
                (String) report.get("USERNAME_EMPLEADO"),
                (String) report.get("EMAIL_EMPLEADO"));
    }

    public EntryReport registerEntry(BaseRequest baseRequest, EmployeeEntryRequest request) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_TALENTO_EMPLEADO_INGRESO");

        LocalDate fchInicioContrato = Common.formatDate(request.getFchInicioContrato());
        LocalDate fchTerminoContrato = Common.formatDate(request.getFchTerminoContrato());

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_REQUERIMIENTO", request.getIdRequerimiento())
                .addValue("ID_TALENTO", request.getIdTalento())
                // TALENTO EMPLEADO
                .addValue("ID_CLIENTE", request.getIdCliente())
                .addValue("ID_AREA", request.getIdArea())
                .addValue("CARGO", request.getCargo())
                .addValue("FCH_INICIO_CONTRATO", fchInicioContrato)
                .addValue("FCH_TERMINO_CONTRATO", fchTerminoContrato)
                .addValue("PROYECTO_SERVICIO", request.getProyectoServicio())
                .addValue("OBJETO_CONTRATO", request.getObjetoContrato())
                .addValue("REMUNERACION", request.getRemuneracion())
                .addValue("ID_TIEMPO_CONTRATO", request.getIdTiempoContrato())
                .addValue("TIEMPO_CONTRATO", request.getTiempoContrato())
                .addValue("ID_MODALIDAD_CONTRATO", request.getIdModalidadContrato())
                .addValue("HORARIO", request.getHorario())
                .addValue("TIENE_EQUIPO", request.getTieneEquipo())
                .addValue("UBICACION", request.getUbicacion())
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
                (String) report.get("AREA"),
                (String) report.get("FCH_HISTORIAL"),
                (String) report.get("MODALIDAD"),
                (String) report.get("MOTIVO"),
                (String) report.get("CARGO"),
                (String) report.get("HORARIO"),
                (String) report.get("MONTO_BASE"),
                (String) report.get("MONTO_MOVILIDAD"),
                (String) report.get("MONTO_TRIMESTRAL"),
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
                (String) report.get("EMAIL_EMPLEADO"));
    }

    public IReport getLastEmployeeHistoryRegister(BaseRequest baseRequest, Integer idTipoHistorial, Integer idTalento) {
        try {
            this.logger.info("Fetching last employee history register for TipoHistorial: {} and Talento: {}",
                    idTipoHistorial, idTalento);
            Map<String, Object> result = executeProcedure(baseRequest, "SP_HISTORIAL_SEL", params -> {
                params.addValue("ID_TIPO_HISTORIAL", idTipoHistorial)
                        .addValue("ID_TALENTO", idTalento);
            });

            List<Map<String, Object>> message = (List<Map<String, Object>>) result.get("#result-set-1");

            if (message != null && !message.isEmpty()) {
                Map<String, Object> row = message.get(0);
                Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
                String mensaje = (String) row.get("MENSAJE");

                BaseResponse response = new BaseResponse(idTipoMensaje, mensaje);

                if (idTipoMensaje == 2) {
                    List<Map<String, Object>> reportData = (List<Map<String, Object>>) result.get("#result-set-2");

                    if (reportData == null || reportData.isEmpty()) {
                        String tipo = switch (idTipoHistorial) {
                            case 1 -> "ingreso";
                            case 2 -> "movimiento";
                            case 3 -> "cese";
                            default -> "historial";
                        };

                        return new BaseReport(
                                new BaseResponse(1, "El talento no tiene reportes de " + tipo + " previos"));
                    }

                    Map<String, Object> reportRow = reportData.get(0);

                    return switch (idTipoHistorial) {
                        case 1 -> mapToEntryReport(response, reportRow);
                        case 2 -> mapToMovementReport(response, reportRow);
                        case 3 -> mapToCeseReport(response, reportRow);
                        default -> new BaseReport(response);
                    };
                } else {
                    return new BaseReport(response);
                }
            }
            this.logger.error("No message returned from stored procedure SP_HISTORIAL_SEL");
            this.logger.error("Error: {}", result);
            return new BaseReport(new BaseResponse(3, "Error al realizar la consulta"));

        } catch (Exception ex) {
            this.logger.error("Error in getLastHistory: ", ex);
            return new BaseReport(new BaseResponse(3, "Error al realizar la consulta"));
        }
    }

    /**
     * Get last solicitud equipo report for a talent
     * 
     * @param baseRequest
     * @param talentId
     * @return
     */
    public SolicitudEquipoReport getLastSolicitudEquipo(BaseRequest baseRequest, Integer talentId) {
        try {
            Map<String, Object> result = executeProcedure(baseRequest, "SP_EQUIPO_SOLICITUD_SEL_LAST", params -> {
                params.addValue("ID_TALENTO", talentId);
                params.addValue("ID_ROL", baseRequest.getIdRol());
                params.addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());
                params.addValue("ID_USUARIO", baseRequest.getIdUsuario());
                params.addValue("ID_EMPRESA", baseRequest.getIdEmpresa());
            });

            List<Map<String, Object>> message = (List<Map<String, Object>>) result.get("#result-set-1");
            SolicitudEquipoReport report = new SolicitudEquipoReport();

            if (message != null && !message.isEmpty()) {
                Map<String, Object> row = message.get(0);
                Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
                String mensaje = (String) row.get("MENSAJE");

                BaseResponse baseResponse = new BaseResponse(idTipoMensaje, mensaje);

                if (idTipoMensaje == 2) {
                    List<Map<String, Object>> resultSetSolicitud = (List<Map<String, Object>>) result
                            .get("#result-set-2");
                    List<Map<String, Object>> resultSetSolicitudSoftwareList = (List<Map<String, Object>>) result
                            .get("#result-set-3");
                    List<Map<String, Object>> resultSetGestor = (List<Map<String, Object>>) result.get("#result-set-4");

                    boolean validSolicitud = resultSetSolicitud != null && !resultSetSolicitud.isEmpty();
                    boolean validGestor = resultSetGestor != null && !resultSetGestor.isEmpty();

                    if (validSolicitud && validGestor) {
                        Map<String, Object> reportRow = resultSetSolicitud.get(0);

                        // solicitud
                        report.setNombreEmpleado((String) reportRow.get("NOMBRE_EMPLEADO"));
                        report.setApellidosEmpleado((String) reportRow.get("APELLIDOS_EMPLEADO"));
                        report.setCliente((String) reportRow.get("EMPRESA_CLIENTE"));
                        report.setArea((String) reportRow.get("AREA"));
                        report.setPuesto((String) reportRow.get("PUESTO"));
                        report.setFechaSolicitud((String) reportRow.get("FECHA_SOLICITUD"));
                        report.setFechaEntrega((String) reportRow.get("FECHA_ENTREGA"));
                        report.setIdTipoEquipo((Integer) reportRow.get("ID_TIPO_EQUIPO"));
                        report.setProcesador((String) reportRow.get("PROCESADOR"));
                        report.setRam((String) reportRow.get("RAM"));
                        report.setHd((String) reportRow.get("HD"));
                        report.setMarca((String) reportRow.get("MARCA"));
                        report.setIdAnexo((Integer) reportRow.get("ID_ANEXO"));
                        report.setCelular((Boolean) reportRow.get("CELULAR"));
                        report.setInternetMovil((Boolean) reportRow.get("INTERNET_MOVIL"));
                        report.setAccesorios((String) reportRow.get("ACCESORIOS"));

                        // lista software
                        List<SolicitudSoftwareRequest> lstSoftware = new ArrayList<>();

                        if (resultSetSolicitudSoftwareList != null && !resultSetSolicitudSoftwareList.isEmpty()) {
                            for (Map<String, Object> softwareRow : resultSetSolicitudSoftwareList) {
                                SolicitudSoftwareRequest software = new SolicitudSoftwareRequest();
                                software.setProducto((String) softwareRow.get("PRODUCTO"));
                                software.setProdVersion((String) softwareRow.get("PROD_VERSION"));

                                lstSoftware.add(software);
                            }
                        }

                        report.setLstSoftware(lstSoftware);

                        // datos gestor
                        Map<String, Object> gestorRow = resultSetGestor.get(0);

                        report.setCorreoGestor((String) gestorRow.get("EMAIL"));
                        report.setNombreApellidoGestor((String) gestorRow.get("NOMBRE_APELLIDO_GESTOR"));
                    } else {
                        this.logger.error(
                                "Datos incompletos para idSolicitudEquipo: {} - validSolicitud: {}, validGestor: {}",
                                talentId, validSolicitud, validGestor);
                        baseResponse = new BaseResponse(3, "Datos incompletos en la solicitud");
                    }
                }

                // base response
                report.setBaseResponse(baseResponse);

                return report;
            }

            report.setBaseResponse(new BaseResponse(3, "Error al realizar la consulta"));
            return report;
        } catch (Exception ex) {
            this.logger.error("Error on last history: {}", ex.getMessage());
            this.logger.error("Stack trace: ", ex);
            SolicitudEquipoReport report = new SolicitudEquipoReport();
            report.setBaseResponse(new BaseResponse(3, "Error al realizar la consulta", ex.getMessage()));
            return report;
        }
    }
}
