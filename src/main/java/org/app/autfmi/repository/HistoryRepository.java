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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
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
    public OperationResult<Integer> registerMovement(BaseRequest baseRequest, EmployeeMovementRequest request) {
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

        if (message == null || message.isEmpty()) {
            this.logger.error("No message returned from stored procedure SP_TALENTO_EMPLEADO_MOVIMIENTO");
            this.logger.error("Error: {}", result);
            BaseResponse baseResponse = new BaseResponse(3, "Error al realizar la consulta",
                    "No se obtuvo respuesta de la base de datos");
            return new OperationResult<>(baseResponse, null);
        }

        Map<String, Object> row = message.get(0);
        Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
        String mensaje = (String) row.get("MENSAJE");
        Integer operationId = (Integer) row.get("ID_OPERACION");
        BaseResponse baseResponse = new BaseResponse(idTipoMensaje, mensaje);

        if (idTipoMensaje != 2)
            return new OperationResult<>(baseResponse, null);

        return new OperationResult<>(baseResponse, operationId);

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
                    .addValue("FCH_HISTORIAL", request.getFchCese())
                    .addValue("ID_CONTRATO", request.getContractId())
                    .addValue("FCH_DEVOLUCION_EQUIPO", request.getFchDevolucionEquipo());
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

    /** Deshace el último cese de un talento. SP_TALENTO_EMPLEADO_CESE_UNDO. */
    public BaseResponse undoContractTermination(BaseRequest baseRequest, Integer idHistorial, Integer idTalento) {
        Map<String, Object> result = executeProcedure(baseRequest, "SP_TALENTO_EMPLEADO_CESE_UNDO",
                params -> params
                        .addValue("ID_HISTORIAL", idHistorial)
                        .addValue("ID_TALENTO", idTalento));
        return mapMessageResponse(result, "SP_TALENTO_EMPLEADO_CESE_UNDO");
    }

    /** Deshace (baja lógica) una solicitud de equipo. SP_EQUIPO_SOLICITUD_DEL. */
    public BaseResponse deleteEquipmentRequest(BaseRequest baseRequest, Integer idSolicitud, Integer idTalento) {
        Map<String, Object> result = executeProcedure(baseRequest, "SP_EQUIPO_SOLICITUD_DEL",
                params -> params
                        .addValue("ID_EQUIPO_SOLICITUD", idSolicitud)
                        .addValue("ID_TALENTO", idTalento));
        return mapMessageResponse(result, "SP_EQUIPO_SOLICITUD_DEL");
    }

    private BaseResponse mapMessageResponse(Map<String, Object> result, String sp) {
        List<Map<String, Object>> rs = (List<Map<String, Object>>) result.get("#result-set-1");
        if (rs == null || rs.isEmpty()) {
            this.logger.error("No message returned from stored procedure {}", sp);
            return new BaseResponse(3, "No se obtuvo respuesta de la base de datos");
        }
        Integer messageId = (Integer) rs.get(0).get("ID_TIPO_MENSAJE");
        String messageText = (String) rs.get(0).get("MENSAJE");
        return new BaseResponse(messageId, messageText);
    }

    public IReport getHistoryReport(
            BaseRequest baseRequest,
            Integer talentId,
            Integer reportTypeRequest,
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
                (String) report.get("EMAIL_EMPLEADO"),
                (String) report.get("FCH_DEVOLUCION_EQUIPO"));
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

    public SolicitudEquipoReport getSolicitudEquipoReport(
            BaseRequest baseRequest,
            Integer talentId,
            @Nullable Integer operationId,
            @NonNull Boolean isLast) {

        this.logger.info("Fetching solicitud equipo report for Talento: {} OperationId: {}", talentId, operationId);

        Map<String, Object> rs = executeProcedure(baseRequest, "SP_FMI_REPORTE_EQUIPO", params -> {
            params
                    .addValue("ID_TALENTO", talentId)
                    .addValue("ID_OPERACION", operationId)
                    .addValue("ULTIMO", isLast);
        });

        if (rs == null || rs.isEmpty()) {
            this.logger.error("No message returned from stored procedure SP_FMI_REPORTE_GENERAL");
            this.logger.error("Error: {}", rs);
            throw new RuntimeException("Error al realizar la consulta");
        }

        List<Map<String, Object>> messageList = (List<Map<String, Object>>) rs.get("#result-set-1");

        if (messageList == null || messageList.isEmpty()) {
            this.logger.error("No message returned from stored procedure SP_FMI_REPORTE_GENERAL");
            throw new RuntimeException("Error al realizar la consulta");
        }

        Map<String, Object> baseResponseDb = messageList.get(0);

        Integer messageId = (Integer) baseResponseDb.get("ID_TIPO_MENSAJE");
        String messageText = (String) baseResponseDb.get("MENSAJE");
        BaseResponse baseResponse = new BaseResponse(messageId, messageText);
        SolicitudEquipoReport report = new SolicitudEquipoReport();
        report.setBaseResponse(baseResponse);

        if (baseResponse.getIdTipoMensaje() != 2)
            return report;

        List<Map<String, Object>> solicitanteList = (List<Map<String, Object>>) rs.get("#result-set-2");
        Map<String, Object> solicitanteDb = solicitanteList.get(0);

        String fullname = (String) solicitanteDb.get("NOMBRES_COMPLETOS");
        String mail = (String) solicitanteDb.get("CORREO");
        var firma = (String) solicitanteDb.get("FIRMA");

        report.setNombreApellidoGestor(fullname);
        report.setCorreoGestor(mail);
        report.setFirmaGestor(firma);

        List<Map<String, Object>> solicitudList = (List<Map<String, Object>>) rs.get("#result-set-3");

        if (solicitanteList == null || solicitanteList.isEmpty()) {
            BaseResponse br = new BaseResponse(1, "Talento no tiene solicitud de equipo");
            report.setBaseResponse(br);
            return report;
        }

        Map<String, Object> solicitudDb = solicitudList.get(0);

        report.setNombreEmpleado((String) solicitudDb.get("NOMBRE_EMPLEADO"));
        report.setApellidosEmpleado((String) solicitudDb.get("APELLIDOS_EMPLEADO"));
        report.setCliente((String) solicitudDb.get("EMPRESA_CLIENTE"));
        report.setArea((String) solicitudDb.get("AREA"));
        report.setPuesto((String) solicitudDb.get("PUESTO"));
        report.setFechaSolicitud((String) solicitudDb.get("FECHA_SOLICITUD"));
        report.setFechaEntrega((String) solicitudDb.get("FECHA_ENTREGA"));

        report.setIdTipoEquipo((Integer) solicitudDb.get("ID_TIPO_EQUIPO"));
        report.setProcesador((String) solicitudDb.get("PROCESADOR"));
        report.setIdAnexo((Integer) solicitudDb.get("ID_ANEXO"));
        report.setRam((String) solicitudDb.get("RAM"));
        report.setHd((String) solicitudDb.get("HD"));
        report.setMarca((String) solicitudDb.get("MARCA"));

        report.setCelular((Boolean) solicitudDb.get("CELULAR"));
        report.setInternetMovil((Boolean) solicitudDb.get("INTERNET_MOVIL"));

        report.setAccesorios((String) solicitudDb.get("ACCESORIOS"));

        // Campos de teléfono y DNI
        report.setDniTalento((String) solicitudDb.get("DNI"));
        report.setCelularTalento((String) solicitudDb.get("NUM_CELULAR"));

        // Mapear software
        List<Map<String, Object>> softwareList = (List<Map<String, Object>>) rs.get("#result-set-4");
        AtomicInteger index = new AtomicInteger(1);

        List<SolicitudSoftwareRequest> software = softwareList.stream()
                .map(row -> {
                    SolicitudSoftwareRequest item = new SolicitudSoftwareRequest();
                    item.setIdItem(index.getAndIncrement());
                    item.setProducto((String) row.get("PRODUCTO"));
                    item.setProdVersion((String) row.get("PROD_VERSION"));
                    return item;
                })
                .collect(Collectors.toList());

        report.setLstSoftware(software);
        return report;
    }
}
