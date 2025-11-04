package org.app.autfmi.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.sqlserver.jdbc.SQLServerDataTable;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.*;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.SolicitudData;
import org.app.autfmi.model.report.SolicitudEquipoReport;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.FileResponse;
import org.app.autfmi.model.response.RequirementListResponse;
import org.app.autfmi.model.response.RequirementResponse;
import org.app.autfmi.model.response.TalentRequirementDataResponse;
import org.app.autfmi.model.response.VacanteCarreraResponse;
import org.app.autfmi.model.response.VacanteSkillsResponse;
import org.app.autfmi.service.impl.MailService;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.FileUtils;
import org.app.autfmi.util.MailUtils;
import org.app.autfmi.util.PDFUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RequirementRepository {

    @NonNull
    private final JdbcTemplate jdbcTemplate;
    private final MailUtils mailUtils;
    private final PDFUtils pdfUtils;
    private final MailService mailService;
    private static final Logger logger = LoggerFactory.getLogger(RequirementRepository.class);

    public BaseResponse listRequirements(BaseRequest baseRequest, Integer nPag, Integer cPag, Integer idCliente,
            String buscar, Date fechaSolicitud, Integer estado) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_REQUERIMIENTO_LST");

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_CLIENTE", idCliente)
                .addValue("BUSCAR", buscar)
                .addValue("FECHA_SOLICITUD", fechaSolicitud)
                .addValue("ESTADO", estado)
                .addValue("NUM_PAG", nPag)
                .addValue("CANT_PAG", cPag)
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
            Integer totalElementos = (Integer) row.get("TOTAL_ELEMENTOS");
            Integer totalPaginas = (Integer) row.get("TOTAL_PAGINAS");

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> requirementSet = (List<Map<String, Object>>) result.get("#result-set-2");
                List<RequirementItemDTO> requirementList = new ArrayList<>();
                if (requirementSet != null && !requirementSet.isEmpty()) {
                    for (Map<String, Object> requirementRow : requirementSet) {
                        requirementList.add(mapToRequirementItemDTO(requirementRow));
                    }
                }
                return new RequirementListResponse(idTipoMensaje, mensaje, requirementList, totalElementos,
                        totalPaginas);
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public BaseResponse getRequirementById(
            Integer idRequerimiento,
            Boolean showfiles,
            Boolean showVacantesList,
            Boolean showContactList,
            BaseRequest baseRequest) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_REQUERIMIENTO_SEL");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_REQUERIMIENTO", idRequerimiento)
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
                List<Map<String, Object>> resultSet2 = (List<Map<String, Object>>) result.get("#result-set-2");

                if (resultSet2 != null && !resultSet2.isEmpty()) {
                    Map<String, Object> requirementData = resultSet2.get(0);

                    List<Map<String, Object>> resultSet3 = (List<Map<String, Object>>) result.get("#result-set-3");
                    List<RequirementTalentDTO> lstRqTalents = new ArrayList<>();

                    if (resultSet3 != null && !resultSet3.isEmpty()) {
                        for (Map<String, Object> rqTalentRow : resultSet3) {
                            RequirementTalentDTO itemRqTalento = new RequirementTalentDTO(
                                    (Integer) rqTalentRow.get("ID_TALENTO"),
                                    (String) rqTalentRow.get("NOMBRES_TALENTO"),
                                    (String) rqTalentRow.get("APELLIDOS_TALENTO"),
                                    (String) rqTalentRow.get("DNI"),
                                    (String) rqTalentRow.get("CELULAR"),
                                    (String) rqTalentRow.get("EMAIL"),
                                    (Integer) rqTalentRow.get("ID_SITUACION"),
                                    (String) rqTalentRow.get("SITUACION"),
                                    (Integer) rqTalentRow.get("ID_ESTADO"),
                                    (String) rqTalentRow.get("ESTADO"),
                                    (Integer) rqTalentRow.get("ID_PERFIL"),
                                    (String) rqTalentRow.get("PERFIL"),
                                    (Boolean) rqTalentRow.get("CONFIRMADO"),
                                    (String) rqTalentRow.get("TOOL_TIP"),
                                    (Integer) rqTalentRow.get("TIENE_EQUIPO"),
                                    (String) rqTalentRow.get("UBICACION"),
                                    (Integer) rqTalentRow.get("ID_MODALIDAD_CONTRATO"),
                                    (String) rqTalentRow.get("FCH_INICIO_LABORES"),
                                    (String) rqTalentRow.get("FCH_FIN_LABORES"),
                                    (BigDecimal) rqTalentRow.get("MONTO_BASE"),
                                    (Integer) rqTalentRow.get("ID_CV_FILE"),
                                    (Integer) rqTalentRow.get("ID_CV_FILE_ES"),
                                    (Integer) rqTalentRow.get("ID_CV_FILE_EN"));

                            lstRqTalents.add(itemRqTalento);
                        }
                    }

                    List<RequirementFileDTO> lstRqFiles = new ArrayList<>();
                    if (showfiles) {
                        List<Map<String, Object>> resultSet4 = (List<Map<String, Object>>) result.get("#result-set-4");

                        if (resultSet4 != null && !resultSet4.isEmpty()) {
                            for (Map<String, Object> rqFileRow : resultSet4) {
                                RequirementFileDTO itemRqArchivo = new RequirementFileDTO(
                                        (Integer) rqFileRow.get("ID_REQUERIMIENTO_ARCHIVO"),
                                        // FileUtils.cargarArchivo((String) rqFileRow.get("LINK")),
                                        "",
                                        (String) rqFileRow.get("NOMBRE_ARCHIVO"),
                                        (Integer) rqFileRow.get("ID_TIPO_ARCHIVO"),
                                        (Integer) rqFileRow.get("ID_TIPO_ARCHIVO_RQ"));
                                lstRqFiles.add(itemRqArchivo);
                            }
                        }
                    }

                    List<RequirementVacanteDTO> lstRqVacantes = new ArrayList<>();
                    if (showVacantesList) {
                        List<Map<String, Object>> resultSet5 = (List<Map<String, Object>>) result.get("#result-set-5");

                        if (resultSet5 != null && !resultSet5.isEmpty()) {
                            for (Map<String, Object> vacante : resultSet5) {
                                RequirementVacanteDTO itemRqVacante = new RequirementVacanteDTO(
                                        (Integer) vacante.get("ID_REQUERIMIENTO_VACANTE"),
                                        (Integer) vacante.get("ID_PERFIL"),
                                        (String) vacante.get("PERFIL_PROFESIONAL"),
                                        (Integer) vacante.get("CANTIDAD"),
                                        (Integer) vacante.get("TOTAL_HABILIDADES"),
                                        (Integer) vacante.get("TOTAL_CARRERAS"),
                                        (BigDecimal) vacante.get("TARIFA_INICIAL"),
                                        (BigDecimal) vacante.get("TARIFA_FINAL"));

                                lstRqVacantes.add(itemRqVacante);
                            }
                        }
                    }

                    List<ClientContactItemDTO> lstContactos = new ArrayList<>();
                    if (showContactList) {
                        List<Map<String, Object>> resultSet6 = (List<Map<String, Object>>) result.get("#result-set-6");

                        if (resultSet6 != null && !resultSet6.isEmpty()) {
                            for (Map<String, Object> contacto : resultSet6) {
                                ClientContactItemDTO itemContacto = new ClientContactItemDTO(
                                        (Integer) contacto.get("ID_CLIENTE_CONTACTO"),
                                        (String) contacto.get("NOMBRES"),
                                        (String) contacto.get("APELLIDO_PATERNO"),
                                        (String) contacto.get("APELLIDO_MATERNO"),
                                        (String) contacto.get("CARGO"),
                                        (String) contacto.get("TELEFONO"),
                                        (String) contacto.get("TELEFONO_2"),
                                        (String) contacto.get("CORREO"),
                                        (String) contacto.get("CORREO_2"),
                                        (Integer) contacto.get("ASIGNADO"));

                                lstContactos.add(itemContacto);
                            }
                        }
                    }

                    // ========================================
                    // Bloque: Facturación
                    // ========================================
                    List<RQFacturacionDTO> lstFacturacion = new ArrayList<>();

                    List<Map<String, Object>> resultSet7 = (List<Map<String, Object>>) result.get("#result-set-7");
                    if (resultSet7 != null && !resultSet7.isEmpty()) {
                        for (Map<String, Object> factRow : resultSet7) {
                            RQFacturacionDTO itemFact = new RQFacturacionDTO(
                                    (Integer) factRow.get("ID_REQUERIMIENTO_FACTURACION"),
                                    (Integer) factRow.get("ID_REQUERIMIENTO"),
                                    (Integer) factRow.get("ID_MODALIDAD"),
                                    (Integer) factRow.get("ID_GRUPO_MODALIDAD"),
                                    (Boolean) factRow.get("DECLARA_SUNAT"),
                                    (String) factRow.get("SEDE_SUNAT"),
                                    (BigDecimal) factRow.get("MONTO_BASE"),
                                    (BigDecimal) factRow.get("MONTO_MOVILIDAD"),
                                    (BigDecimal) factRow.get("MONTO_MENSUAL"),
                                    (BigDecimal) factRow.get("MONTO_TRIMESTRAL"),
                                    (BigDecimal) factRow.get("MONTO_SEMESTRAL"),
                                    (Integer) factRow.get("ID_ESTADO_REGISTRO"),
                                    (String) factRow.get("MODALIDAD"),
                                    (String) factRow.get("GRUPO_MODALIDAD"));

                            lstFacturacion.add(itemFact);
                        }
                    }

                    return new RequirementResponse(
                            idTipoMensaje,
                            mensaje,
                            mapToRequirementDTO(requirementData, lstRqTalents, lstRqFiles, lstRqVacantes,
                                    lstContactos,
                                    lstFacturacion));
                }
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    private SQLServerDataTable loadLstFacturacionTable(List<RQFacturacionDTO> lstFacturacion)
            throws SQLServerException {
        SQLServerDataTable table = new SQLServerDataTable();

        table.addColumnMetadata("ID_REQUERIMIENTO_FACTURACION", Types.INTEGER);
        table.addColumnMetadata("ID_REQUERIMIENTO", Types.INTEGER);
        table.addColumnMetadata("ID_MODALIDAD", Types.INTEGER);
        table.addColumnMetadata("ID_GRUPO_MODALIDAD", Types.INTEGER);
        table.addColumnMetadata("DECLARA_SUNAT", Types.BIT);
        table.addColumnMetadata("SEDE_SUNAT", Types.VARCHAR);
        table.addColumnMetadata("MONTO_BASE", Types.DOUBLE);
        table.addColumnMetadata("MONTO_MOVILIDAD", Types.DOUBLE);
        table.addColumnMetadata("MONTO_MENSUAL", Types.DOUBLE);
        table.addColumnMetadata("MONTO_TRIMESTRAL", Types.DOUBLE);
        table.addColumnMetadata("MONTO_SEMESTRAL", Types.DOUBLE);
        table.addColumnMetadata("ID_ESTADO_REGISTRO", Types.INTEGER);

        for (RQFacturacionDTO facturacion : lstFacturacion) {
            table.addRow(
                    facturacion.getIdRequerimientoFacturacion(),
                    facturacion.getIdRequerimiento(),
                    facturacion.getIdModalidad(),
                    facturacion.getIdGrupoModalidad(),
                    facturacion.getDeclaraSunat(),
                    facturacion.getSedeSunat(),
                    facturacion.getMontoBase(),
                    facturacion.getMontoMovilidad(),
                    facturacion.getMontoMensual(),
                    facturacion.getMontoTrimestral(),
                    facturacion.getMontoSemestral(),
                    facturacion.getIdEstadoRegistro());
        }

        return table;
    }

    public BaseResponse saveRequirement(RequirementRequest request, BaseRequest baseRequest) throws SQLServerException {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_REQUERIMIENTO_INS");
        SQLServerDataTable tvpRqFiles = loadTvpRequirementFiles(request.getLstArchivos(), baseRequest.getIdEmpresa());
        SQLServerDataTable tvpRqVacantes = loadTvpRequirementVacantes(request.getLstVacantes());
        SQLServerDataTable tvpRqVacSkill = loadTvpRqVacSkill(request.getLstVacanteSkills());
        SQLServerDataTable tvpCarreras = loadTvpLstCarreras(request.getLstCarreras());

        SQLServerDataTable tvpFacturacion = loadLstFacturacionTable(request.getLstFacturacion());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_CLIENTE", request.getIdCliente())
                .addValue("CLIENTE", request.getCliente())
                .addValue("TITULO", request.getTitulo())
                .addValue("CODIGO_RQ", request.getCodigoRQ())
                .addValue("FECHA_SOLICITUD", request.getFechaSolicitud())
                .addValue("DESCRIPCION", request.getDescripcion())
                .addValue("ESTADO", request.getEstado())
                .addValue("AUTOGEN_RQ", request.getAutogenRQ())
                .addValue("ID_DURACION", request.getIdDuracion())
                .addValue("DURACION", request.getDuracion())
                .addValue("FECHA_VENCIMIENTO", request.getFechaVencimiento())
                .addValue("ID_MODALIDAD", request.getIdModalidad())
                .addValue("LST_CONTACTOS", request.getLstContactos())
                .addValue("LST_VACANTES", tvpRqVacantes)
                .addValue("LST_ARCHIVOS", tvpRqFiles)
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("USUARIO", baseRequest.getUsername())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("MODALIDAD_FACT", request.getIdModalidadFact())
                .addValue("LST_VACANTE_SKILLS", tvpRqVacSkill)
                .addValue("LST_VACANTE_CARRERAS", tvpCarreras)
                // Duracion de contrato
                .addValue("ID_DUR_CONTRATO", request.getIdDuracionContrato())
                .addValue("DURACION_CONTRATO", request.getDuracionContrato())

                // Facturacion asociada a la modalidad de contrato
                .addValue("LST_FACTURACION", tvpFacturacion);

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");
            if (idTipoMensaje == 2) {

                Integer idNuevoRQ = (Integer) row.get("ID_NEW_RQ");
                guardarArchivos(request.getLstArchivos(), idNuevoRQ, baseRequest.getIdEmpresa());

                String rqCode = (String) row.get("CODIGO_RQ");

                // Datos RQ
                var rDto = new RequirementDTO();
                rDto.setTitulo(request.getTitulo());
                rDto.setCodigoRQ(rqCode);
                rDto.setFechaSolicitud(request.getFechaSolicitud());
                rDto.setFechaVencimiento(request.getFechaVencimiento());
                rDto.setDescripcion(request.getDescripcion());
                rDto.setIdEstado(request.getEstado());

                // Cliente - nombre
                rDto.setCliente(request.getCliente());

                // Gestion
                rDto.setDuracion(
                        request.getDuracion() != null ? BigDecimal.valueOf(request.getDuracion()) : BigDecimal.ZERO);

                rDto.setIdDuracion(request.getIdDuracion());
                rDto.setIdModalidad(request.getIdModalidad());
                rDto.setModalidadFact(request.getIdModalidadFact());

                // Vacantes
                List<Map<String, Object>> vacantesResultSet = (List<Map<String, Object>>) result.get("#result-set-2");
                List<Map<String, Object>> vacantesMapList = new ArrayList<>();

                if (vacantesResultSet != null && !vacantesResultSet.isEmpty()) {
                    for (Map<String, Object> vacante : vacantesResultSet) {
                        Map<String, Object> vacanteMap = new HashMap<>();

                        vacanteMap.put("idPerfil", vacante.get("ID_REQUERIMIENTO_VACANTE"));
                        vacanteMap.put("perfil", vacante.get("PERFIL_PROFESIONAL"));
                        vacanteMap.put("cantidad", vacante.get("CANTIDAD"));

                        vacantesMapList.add(vacanteMap);
                    }

                }

                // Contactos
                List<Map<String, Object>> contactosResultSet = (List<Map<String, Object>>) result.get("#result-set-3");
                List<Map<String, Object>> contactosMapList = new ArrayList<>();

                if (contactosResultSet != null && !contactosResultSet.isEmpty()) {
                    for (Map<String, Object> contacto : contactosResultSet) {
                        Map<String, Object> contactoMap = new HashMap<>();
                        String nombreCompleto = String.format("%s %s %s",
                                contacto.getOrDefault("NOMBRES", ""),
                                contacto.getOrDefault("APELLIDO_PATERNO", ""),
                                contacto.getOrDefault("APELLIDO_MATERNO", "")).trim();

                        contactoMap.put("nombre", nombreCompleto);
                        contactoMap.put("celular", contacto.get("TELEFONO"));
                        contactoMap.put("correo", contacto.get("CORREO"));
                        contactoMap.put("cargo", contacto.get("CARGO"));

                        contactosMapList.add(contactoMap);
                    }
                }

                // --- Habilidades técnicas por vacante (result set 4)
                List<Map<String, Object>> habilidadesResultSet = (List<Map<String, Object>>) result
                        .get("#result-set-4");

                // --- Carreras por vacante (result set 5)
                List<Map<String, Object>> carrerasResultSet = (List<Map<String, Object>>) result.get("#result-set-5");

                // --- Correo del ejecutor (result set 6)
                List<Map<String, Object>> correoResultSet = (List<Map<String, Object>>) result.get("#result-set-6");
                String correoEjecutor = null;
                if (correoResultSet != null && !correoResultSet.isEmpty()) {
                    correoEjecutor = (String) correoResultSet.get(0).get("CORREO");
                }

                // send mail
                mailService.sendCreateRequirementNotification(
                        baseRequest.getUsername(),
                        rDto, vacantesMapList,
                        contactosMapList,
                        habilidadesResultSet,
                        carrerasResultSet,
                        correoEjecutor);

            }

            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    private SQLServerDataTable loadTvpLstCarreras(List<VacanteCarreraRequest> carreras) throws SQLServerException {

        SQLServerDataTable tvp = new SQLServerDataTable();
        tvp.addColumnMetadata("ID_PERFIL", Types.INTEGER);
        tvp.addColumnMetadata("CARRERA", Types.VARCHAR);
        tvp.addColumnMetadata("ID_GRADO_ESTUDIOS", Types.INTEGER);
        tvp.addColumnMetadata("OPCIONAL", Types.INTEGER);

        for (VacanteCarreraRequest carrera : carreras) {
            Integer opcional = carrera.getIsOptional() ? 1 : 0;
            tvp.addRow(
                    carrera.getIdPerfil(),
                    carrera.getCarrera(),
                    carrera.getIdGrado(),
                    opcional);
        }
        return tvp;
    }

    public BaseResponse updateRequirement(RequirementRequest request, BaseRequest baseRequest)
            throws SQLServerException {

        logger.info("Inicio REPOSITORY updateRequirement - ID_REQUERIMIENTO: {}", request.getIdRequerimiento());

        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_REQUERIMIENTO_UPD");

        SQLServerDataTable tvpRqVacantes = loadTvpRequirementVacantesUpdate(request.getLstVacantes());

        SQLServerDataTable lstFactuacion = loadLstFacturacionTable(request.getLstFacturacion());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_REQUERIMIENTO", request.getIdRequerimiento())
                .addValue("ID_CLIENTE", request.getIdCliente())
                .addValue("CLIENTE", request.getCliente())
                .addValue("TITULO", request.getTitulo())
                .addValue("CODIGO_RQ", request.getCodigoRQ())
                .addValue("FECHA_SOLICITUD", request.getFechaSolicitud())
                .addValue("DESCRIPCION", request.getDescripcion())
                .addValue("ESTADO", request.getEstado())
                .addValue("ID_DURACION", request.getIdDuracion())
                .addValue("DURACION", request.getDuracion())
                .addValue("FECHA_VENCIMIENTO", request.getFechaVencimiento())
                .addValue("ID_MODALIDAD", request.getIdModalidad())
                .addValue("LST_VACANTES", tvpRqVacantes)
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("USUARIO", baseRequest.getUsername())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("MODALIDAD_FACT", request.getIdModalidadFact())
                .addValue("ID_DUR_CONTRATO", request.getIdDuracionContrato())
                .addValue("DURACION_CONTRATO", request.getDuracionContrato())
                .addValue("LST_FACTURACION", lstFactuacion);

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                // Mapear los resultados para enviar email

                // Datos RQ básicos
                var rDto = new RequirementDTO();
                rDto.setTitulo(request.getTitulo());
                rDto.setCodigoRQ(request.getCodigoRQ());
                rDto.setFechaSolicitud(request.getFechaSolicitud());
                rDto.setFechaVencimiento(request.getFechaVencimiento());
                rDto.setDescripcion(request.getDescripcion());
                rDto.setIdEstado(request.getEstado());
                rDto.setCliente(request.getCliente());
                rDto.setDuracion(
                        request.getDuracion() != null ? BigDecimal.valueOf(request.getDuracion()) : BigDecimal.ZERO);
                rDto.setIdDuracion(request.getIdDuracion());
                rDto.setIdModalidad(request.getIdModalidad());
                rDto.setModalidadFact(request.getIdModalidadFact());

                // Vacantes (result set 2)
                List<Map<String, Object>> vacantesResultSet = (List<Map<String, Object>>) result.get("#result-set-2");
                List<Map<String, Object>> vacantesMapList = new ArrayList<>();

                if (vacantesResultSet != null && !vacantesResultSet.isEmpty()) {
                    for (Map<String, Object> vacante : vacantesResultSet) {
                        Map<String, Object> vacanteMap = new HashMap<>();
                        vacanteMap.put("idPerfil", vacante.get("ID_REQUERIMIENTO_VACANTE"));
                        vacanteMap.put("perfil", vacante.get("PERFIL_PROFESIONAL"));
                        vacanteMap.put("cantidad", vacante.get("CANTIDAD"));
                        vacantesMapList.add(vacanteMap);
                    }
                }

                // Contactos (result set 3)
                List<Map<String, Object>> contactosResultSet = (List<Map<String, Object>>) result.get("#result-set-3");
                List<Map<String, Object>> contactosMapList = new ArrayList<>();

                if (contactosResultSet != null && !contactosResultSet.isEmpty()) {
                    for (Map<String, Object> contacto : contactosResultSet) {
                        Map<String, Object> contactoMap = new HashMap<>();
                        String nombreCompleto = String.format("%s %s %s",
                                contacto.getOrDefault("NOMBRES", ""),
                                contacto.getOrDefault("APELLIDO_PATERNO", ""),
                                contacto.getOrDefault("APELLIDO_MATERNO", "")).trim();

                        contactoMap.put("nombre", nombreCompleto);
                        contactoMap.put("celular", contacto.get("TELEFONO"));
                        contactoMap.put("correo", contacto.get("CORREO"));
                        contactoMap.put("cargo", contacto.get("CARGO"));
                        contactosMapList.add(contactoMap);
                    }
                }

                // Habilidades técnicas por vacante (result set 4)
                List<Map<String, Object>> habilidadesResultSet = (List<Map<String, Object>>) result
                        .get("#result-set-4");

                // Carreras por vacante (result set 5)
                List<Map<String, Object>> carrerasResultSet = (List<Map<String, Object>>) result.get("#result-set-5");

                // Postulantes/talentos (result set 6)
                List<Map<String, Object>> postulantsResultSet = (List<Map<String, Object>>) result.get("#result-set-6");
                List<Map<String, Object>> postulantesList = new ArrayList<>();

                if (postulantsResultSet != null && !postulantsResultSet.isEmpty()) {
                    for (Map<String, Object> postulante : postulantsResultSet) {
                        Map<String, Object> postulanteMap = new HashMap<>();
                        postulanteMap.put("nombres", postulante.get("NOMBRES_TALENTO"));
                        postulanteMap.put("apellidos", postulante.get("APELLIDOS_TALENTO"));
                        postulanteMap.put("dni", postulante.get("DNI"));
                        postulanteMap.put("celular", postulante.get("CELULAR"));
                        postulanteMap.put("correo", postulante.get("EMAIL"));
                        postulanteMap.put("situacion", postulante.get("SITUACION"));
                        postulanteMap.put("estado", postulante.get("ESTADO"));
                        postulanteMap.put("perfil", postulante.get("PERFIL"));
                        postulantesList.add(postulanteMap);
                    }
                }

                // result set 7 correo del usuario ejecutor
                List<Map<String, Object>> correoResultSet = (List<Map<String, Object>>) result.get("#result-set-7");
                String correoEjecutor = null;

                if (correoResultSet != null && !correoResultSet.isEmpty()) {
                    Map<String, Object> rw = correoResultSet.get(0);
                    correoEjecutor = (String) rw.get("CORREO");
                }

                // Enviar email de notificación
                mailService.sendUpdateRequirementNotification(
                        baseRequest.getUsername(),
                        rDto,
                        vacantesMapList,
                        contactosMapList,
                        habilidadesResultSet,
                        carrerasResultSet,
                        postulantesList, correoEjecutor);
            }

            logger.info("Fin REPOSITORY updateRequirement - ID_REQUERIMIENTO: {} - Resultado: {} - {}",
                    request.getIdRequerimiento(), idTipoMensaje, mensaje);
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        logger.info("Fin REPOSITORY updateRequirement - No se obtuvieron resultados");
        return null;
    }

    public BaseResponse saveRequirementTalents(RequirementTalentRequest request, BaseRequest baseRequest) {
        try {
            logger.info("SaveRequirementTalents started - ID_REQUERIMIENTO: {}", request.getIdRequerimiento());

            BaseResponse baseResponse = new BaseResponse();
            SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("SP_REQUERIMIENTO_TALENTO_INS");

            SQLServerDataTable tvpRqTalents = loadTvpRequirementTalents(request);
            SQLServerDataTable tvpProductos = loadTvpProductos(request);

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("ID_REQUERIMIENTO", request.getIdRequerimiento())
                    .addValue("LST_TALENTOS", tvpRqTalents)
                    .addValue("LST_SOFTWARE", tvpProductos)
                    .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                    .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                    .addValue("ID_ROL", baseRequest.getIdRol())
                    .addValue("USUARIO", baseRequest.getUsername())
                    .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());

            System.out.println("EJECUTANDO SP...");
            Map<String, Object> result = simpleJdbcCall.execute(params);

            List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

            System.out.println(resultSet);
            if (resultSet != null && !resultSet.isEmpty()) {
                Map<String, Object> row = resultSet.get(0);
                baseResponse.setIdTipoMensaje((Integer) row.get("ID_TIPO_MENSAJE"));
                baseResponse.setMensaje((String) row.get("MENSAJE"));

                if (baseResponse.getIdTipoMensaje() == 2) {
                    List<Map<String, Object>> gestorRqSet = (List<Map<String, Object>>) result.get("#result-set-2"); // GESTOR
                    List<Map<String, Object>> postulantsSet = (List<Map<String, Object>>) result.get("#result-set-3"); // TALENTOS
                                                                                                                       // CONFIRMADOS
                    List<Map<String, Object>> contactosSet = (List<Map<String, Object>>) result.get("#result-set-4"); // CONTACTOS
                    List<Map<String, Object>> gestorDocsSet = (List<Map<String, Object>>) result.get("#result-set-5"); // Gestor

                    // Gestor del cliente
                    List<Map<String, Object>> clientGs = (List<Map<String, Object>>) result
                            .get("#result-set-6");
                    // DOCUMENTOS
                    List<Map<String, Object>> reportSet = (List<Map<String, Object>>) result.get("#result-set-7"); // REPORTES
                    List<Map<String, Object>> solicitudesEquipoSet = (List<Map<String, Object>>) result
                            .get("#result-set-8"); // REPORTE SOLICITUDES EQUIPO
                    List<Map<String, Object>> equipoSoftwaresSet = (List<Map<String, Object>>) result
                            .get("#result-set-9"); // REPORTE EQUIPO SOFTWARES

                    // Mapear los datos del gestor
                    GestorDTO gs = null;
                    if (clientGs != null && !clientGs.isEmpty()) {
                        Map<String, Object> gsRow = clientGs.get(0);
                        String gsSignature = (String) gsRow.get("NOMBRE_FIRMANTE");
                        String gsFullname = (String) gsRow.get("NOMBRE_FIRMANTE");

                        gs = new GestorDTO(gsSignature, gsFullname);
                    } else {
                        throw new NullPointerException("No se encontró el gestor del cliente");
                    }

                    if (postulantsSet != null && !postulantsSet.isEmpty() && gestorRqSet != null
                            && !gestorRqSet.isEmpty()) {
                        Map<String, Object> gestorRqRow = gestorRqSet.get(0);
                        GestorRqDTO gestorRq = new GestorRqDTO(
                                (String) gestorRqRow.get("NOMBRES"),
                                (String) gestorRqRow.get("APELLIDOS"),
                                (String) gestorRqRow.get("CORREO"),
                                (String) gestorRqRow.get("CODIGO_RQ"),
                                (String) gestorRqRow.get("CLIENTE"),
                                "Ingreso");

                        List<String> copyTo = new ArrayList<>();
                        copyTo.add(gestorRq.getCorreo());

                        Map<String, Object> gestorDocs = gestorDocsSet.get(0);
                        String gestorDocsCorreo = gestorDocs.get("GESTOR_DOCS_CORREO").toString();
                        String gestorDocsFullName = gestorDocs.get("GESTOR_DOCS").toString();

                        List<PostulantDTO> postulantList = new ArrayList<>();
                        if (!postulantsSet.isEmpty()) {
                            for (Map<String, Object> postulantRow : postulantsSet) {
                                postulantList.add(mapListPostulantDTO(postulantRow));
                            }
                        }

                        List<String> contactosList = new ArrayList<>();
                        if (contactosSet != null && !contactosSet.isEmpty()) {
                            for (Map<String, Object> contactoRow : contactosSet) {
                                contactosList.add(
                                        (String) contactoRow.get("CORREO"));
                            }
                        }

                        // ENVIAR CORREO
                        if (request.getFlagCorreo()) {

                            try {
                                mailUtils.sendRequirementPostulantMail(gestorRq, "Ingreso de nuevo talento",
                                        postulantList,
                                        contactosList);
                            } catch (Exception e) {
                                logger.error("Error enviando correo de talentos para RQ: {}. Error: {}",
                                        gestorRq.getCodigoRQ(), e.getStackTrace());
                                return new BaseResponse(2,
                                        "Registro completado, pero falló el envío de correo para talentos confirmados",
                                        e.getMessage());
                            }

                            // ENVIAR CORREO CON REPORTE DE NUEVOS INGRESOS
                            if (reportSet != null && !reportSet.isEmpty()) {
                                List<FileDTO> lstfiles = new ArrayList<>();
                                SolicitudData data = new SolicitudData();

                                try {

                                    for (Map<String, Object> reportRow : reportSet) {
                                        EntryReport report = mapToEntryReport(reportRow);
                                        // FORM FILE
                                        String fullname = report.getNombres() + " " + report.getApellidos();

                                        FileDTO fileFormulario = new FileDTO(
                                                "FT-GT-12-FMI-" + fullname,
                                                pdfUtils.replaceEntryRequestValues(
                                                        pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO),
                                                        report, gs),
                                                null);

                                        // SOLICITUD FILE
                                        data.setNombres(report.getNombres());
                                        data.setApellidos(report.getApellidos());
                                        data.setArea(report.getUnidad());
                                        data.setFechaSolicitud(report.getFechaInicioContrato());

                                        data.setNombresCreacion(report.getNombres());
                                        data.setApellidosCreacion(report.getApellidos());
                                        data.setNombreUsuarioCreacion(report.getUsernameEmpleado());
                                        data.setCorreoCreacion(report.getEmailEmpleado());
                                        data.setAreaCreacion(report.getUnidad());
                                        data.setFirmante(report.getFirmante());

                                        FileDTO fileSolicitud = new FileDTO(
                                                "FT-GS-01-FMI-" + fullname,
                                                pdfUtils.replaceSolicitudPDFValues(
                                                        pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD),
                                                        data, gs),
                                                null);

                                        lstfiles.add(fileFormulario);
                                        lstfiles.add(fileSolicitud);
                                    }

                                    // ENVIAR CORREO CON PDF's
                                    logger.info("Enviando correos con PDF");
                                    if (gestorDocsCorreo != null && !gestorDocsCorreo.isEmpty()) {
                                        pdfUtils.enviarCorreoConPDF(
                                                lstfiles,
                                                gestorDocsCorreo,
                                                copyTo,
                                                "Ingreso de empleado",
                                                "Formulario de nuevo ingreso de empleado.");
                                        logger.info("Correos enviados con PDF");
                                    } else {
                                        logger.error("Gestor de correo no configurado");
                                        throw new NullPointerException("Gestor de correo no configurado");
                                    }

                                } catch (Exception e) {
                                    logger.error("Error enviando correo con PDF de ingreso para RQ: {}. Error: {}",
                                            gestorRq.getCodigoRQ(), e.getStackTrace());
                                    return new BaseResponse(2,
                                            "Registro completado, pero falló el envío de correo con PDF de ingreso",
                                            e.getMessage());
                                }
                            }

                            // Solicitudes Equipo
                            if (solicitudesEquipoSet != null && !solicitudesEquipoSet.isEmpty()) {

                                try {

                                    List<FileDTO> lstSolicitudEquipoFiles = new ArrayList<>();
                                    String template = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD_EQUIPO);

                                    for (Map<String, Object> solicitudRow : solicitudesEquipoSet) {
                                        SolicitudEquipoReport report = new SolicitudEquipoReport();
                                        // datos gestor
                                        report.setCorreoGestor(gestorDocsCorreo);
                                        report.setNombreApellidoGestor(gestorDocsFullName);
                                        // datos reporte
                                        report.setNombreEmpleado((String) solicitudRow.get("NOMBRE_EMPLEADO"));
                                        report.setApellidosEmpleado((String) solicitudRow.get("APELLIDOS_EMPLEADO"));
                                        report.setCliente((String) solicitudRow.get("CLIENTE"));
                                        report.setArea((String) solicitudRow.get("AREA"));
                                        report.setPuesto((String) solicitudRow.get("PUESTO"));
                                        report.setFechaSolicitud((String) solicitudRow.get("FECHA_SOLICITUD"));
                                        report.setFechaEntrega((String) solicitudRow.get("FECHA_ENTREGA"));
                                        report.setIdTipoEquipo((Integer) solicitudRow.get("ID_TIPO_EQUIPO"));
                                        report.setProcesador((String) solicitudRow.get("PROCESADOR"));
                                        report.setRam((String) solicitudRow.get("RAM"));
                                        report.setHd((String) solicitudRow.get("HD"));
                                        report.setMarca((String) solicitudRow.get("MARCA"));
                                        report.setIdAnexo((Integer) solicitudRow.get("ID_ANEXO"));
                                        report.setCelular((Boolean) solicitudRow.get("CELULAR"));
                                        report.setInternetMovil((Boolean) solicitudRow.get("INTERNET_MOVIL"));
                                        report.setAccesorios((String) solicitudRow.get("ACCESORIOS"));

                                        // lista de software por solicitud
                                        List<SolicitudSoftwareRequest> lstSoftware = new ArrayList<>();
                                        if (equipoSoftwaresSet != null && !equipoSoftwaresSet.isEmpty()) {
                                            for (Map<String, Object> softwareRow : equipoSoftwaresSet) {
                                                Integer solicitudSoftwareId = (Integer) softwareRow
                                                        .get("ID_EQUIPO_SOLICITUD");
                                                Integer solicitudId = (Integer) solicitudRow.get("ID_EQUIPO_SOLICITUD");

                                                // Solo agregar si pertenece a esta solicitud
                                                if (solicitudSoftwareId != null
                                                        && solicitudSoftwareId.equals(solicitudId)) {
                                                    SolicitudSoftwareRequest software = new SolicitudSoftwareRequest();
                                                    software.setIdItem(
                                                            (Integer) softwareRow.get("ID_EQUIPO_SOLICITUD"));
                                                    software.setProducto((String) softwareRow.get("PRODUCTO"));
                                                    software.setProdVersion((String) softwareRow.get("PROD_VERSION"));
                                                    lstSoftware.add(software);
                                                }
                                            }
                                            report.setLstSoftware(lstSoftware);
                                        }
                                        String fullname = report.getNombreEmpleado() + " "
                                                + report.getApellidosEmpleado();
                                        FileDTO fileFormulario = new FileDTO(
                                                "FT-GS-03-FMI-" + fullname,
                                                pdfUtils.replaceSolicitudEquipoPDFValues(template, report, gs),
                                                null);

                                        lstSolicitudEquipoFiles.add(fileFormulario);
                                    }

                                    if (gestorDocsCorreo == null || gestorDocsCorreo.isEmpty()) {
                                        logger.error("Gestor de correo no configurado");
                                        throw new NullPointerException("Gestor de correo no configurado");

                                    }

                                    pdfUtils.enviarCorreoConPDF(
                                            lstSolicitudEquipoFiles,
                                            gestorDocsCorreo,
                                            copyTo,
                                            "Requerimiento de Software y Hardware",
                                            "Formulario Requerimiento de Software y Hardware.");
                                } catch (Exception e) {
                                    logger.error(
                                            "Error enviando correo con PDF de solicitud de equipo para RQ: {}. Error: {}",
                                            gestorRq.getCodigoRQ(), e.getStackTrace());
                                    return new BaseResponse(2,
                                            "Registro completado, pero falló el envío de correo con PDF de solicitud de equipo",
                                            e.getMessage());
                                }
                            }
                        }
                    }
                }
            }

            logger.info("Finished task: saveRequirementTalents");
            return baseResponse;
        } catch (Exception e) {
            logger.error("ERROR REPOSITORY saveRequirementTalents: {}", e.getMessage());
            logger.error("Error{}", e);
            return new BaseResponse(3, "Ha ocurrido un error en el proceso de guardado de talentos.",
                    e.getMessage());
        }
    }

    private EntryReport mapToEntryReport(Map<String, Object> report) {
        return new EntryReport(
                null,
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
                null,
                (String) report.get("FIRMANTE"),
                null,
                (String) report.get("USERNAME_EMPLEADO"),
                (String) report.get("EMAIL_EMPLEADO"));
    }

    public BaseResponse getRequirementTalentData(BaseRequest baseRequest, Integer idTalento, Integer idRequerimiento) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_TALENTO_REQUERIMIENTO_SEL");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_TALENTO", idTalento)
                .addValue("ID_REQUERIMIENTO", idRequerimiento)
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
                List<Map<String, Object>> resultSet2 = (List<Map<String, Object>>) result.get("#result-set-2");

                if (resultSet2 != null && !resultSet2.isEmpty()) {
                    Map<String, Object> talentRequirementData = resultSet2.get(0);

                    return new TalentRequirementDataResponse(idTipoMensaje, mensaje,
                            mapTalentRequirementDataDTO(talentRequirementData));
                }
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public static TalentRequirementDataDTO mapTalentRequirementDataDTO(Map<String, Object> talentoRQ) {
        return new TalentRequirementDataDTO(
                (Integer) talentoRQ.get("ID_TALENTO"),
                (String) talentoRQ.get("NOMBRES"),
                (String) talentoRQ.get("APELLIDOS"),
                (String) talentoRQ.get("DNI"),
                (String) talentoRQ.get("CELULAR"),
                (String) talentoRQ.get("EMAIL"),
                (Integer) talentoRQ.get("ID_SITUACION"),
                (String) talentoRQ.get("SITUACION"),
                (String) talentoRQ.get("TOOL_TIP"),
                (Integer) talentoRQ.get("ID_ESTADO"),
                (String) talentoRQ.get("ESTADO"),
                (Integer) talentoRQ.get("TIENE_EQUIPO"));
    }

    public BaseResponse saveRequirementFile(BaseRequest baseRequest, RequirementFileRequest request)
            throws SQLServerException {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_REQUERIMIENTO_ARCHIVO_INS");
        SQLServerDataTable tvpRqFiles = loadTvpRequirementFiles(request.getLstArchivos(), baseRequest.getIdEmpresa());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_REQUERIMIENTO", request.getIdRequerimiento())
                .addValue("LST_ARCHIVOS", tvpRqFiles)
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
                guardarArchivos(request.getLstArchivos(), request.getIdRequerimiento(), baseRequest.getIdEmpresa());
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public BaseResponse removeRequirementFile(BaseRequest baseRequest, Integer idRqFile) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_REQUERIMIENTO_ARCHIVO_DEL");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_REQUERIMIENTO_ARCHIVO", idRqFile)
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
                List<Map<String, Object>> resultSet2 = (List<Map<String, Object>>) result.get("#result-set-2");
                Map<String, Object> fileToDelete = resultSet2.get(0);
                String rutaPre = (String) fileToDelete.get("LINK");
                FileUtils.eliminarArchivoAws(rutaPre);
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    private RequirementItemDTO mapToRequirementItemDTO(Map<String, Object> requirement) {
        ObjectMapper objectMapper = new ObjectMapper();

        List<RequirementPerfilItemDTO> perfiles = new ArrayList<>();
        String jsonPerfiles = (String) requirement.get("LST_PERFILES");

        if (jsonPerfiles != null && !jsonPerfiles.isEmpty()) {
            try {
                perfiles = objectMapper.readValue(
                        jsonPerfiles,
                        new TypeReference<>() {
                        });
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return new RequirementItemDTO(
                (Integer) requirement.get("ID_REQUERIMIENTO"),
                (String) requirement.get("CLIENTE"),
                (String) requirement.get("TITULO"),
                (String) requirement.get("CODIGO_RQ"),
                (String) requirement.get("FECHA_SOLICITUD"),
                (Integer) requirement.get("ID_ESTADO"),
                (String) requirement.get("ESTADO"),
                (Integer) requirement.get("VACANTES"),
                (Integer) requirement.get("VACANTES_CUBIERTAS"),
                (String) requirement.get("DURACION"),
                (String) requirement.get("FECHA_VENCIMIENTO"),
                (String) requirement.get("MODALIDAD"),
                (Integer) requirement.get("ID_ALERTA"),
                perfiles);
    }

    private RequirementDTO mapToRequirementDTO(
            Map<String, Object> requerimiento,
            List<RequirementTalentDTO> lstRqTalents,
            List<RequirementFileDTO> lstRqFiles,
            List<RequirementVacanteDTO> lstRqVacantes,
            List<ClientContactItemDTO> lstContactos,
            List<RQFacturacionDTO> lstRQFacturacion) {
        return new RequirementDTO(
                (Integer) requerimiento.get("ID_CLIENTE"),
                (String) requerimiento.get("CLIENTE"),
                (String) requerimiento.get("TITULO"),
                (String) requerimiento.get("CODIGO_RQ"),
                requerimiento.get("FECHA_SOLICITUD").toString(),
                (String) requerimiento.get("DESCRIPCION"),
                (Integer) requerimiento.get("ID_ESTADO"),
                (String) requerimiento.get("ESTADO"),
                (Integer) requerimiento.get("VACANTES"),
                (Integer) requerimiento.get("ID_DURACION"),
                (BigDecimal) requerimiento.get("DURACION"),
                requerimiento.get("FECHA_VENCIMIENTO").toString(),
                (Integer) requerimiento.get("ID_MODALIDAD"),
                (String) requerimiento.get("MODALIDAD"),
                (String) requerimiento.get("MODALIDAD_FACT"),
                lstRqVacantes,
                lstRqTalents,
                lstRqFiles,
                lstContactos,
                (Integer) requerimiento.get("ID_DUR_CONTRATO"),
                (BigDecimal) requerimiento.get("DURACION_CONTRATO"),
                lstRQFacturacion);
    }

    private static SQLServerDataTable loadTvpRequirementFiles(List<FileRequest> lstArchivos, Integer idEmpresa)
            throws SQLServerException {
        SQLServerDataTable tvpRqFiles = new SQLServerDataTable();
        tvpRqFiles.addColumnMetadata("INDICE", Types.INTEGER);
        tvpRqFiles.addColumnMetadata("LINK", Types.VARCHAR);
        tvpRqFiles.addColumnMetadata("NOMBRE_ARCHIVO", Types.VARCHAR);
        tvpRqFiles.addColumnMetadata("ID_TIPO_ARCHIVO", Types.INTEGER);
        tvpRqFiles.addColumnMetadata("ID_TIPO_ARCHIVO_RQ", Types.INTEGER);
        int indice = 1;

        for (FileRequest fileRequest : lstArchivos) {
            String rutaArchivo = Constante.RUTA_REPOSITORIO + idEmpresa + Constante.RUTA_RQ_ARCHIVOS
                    + fileRequest.getNombreArchivo() + "." + fileRequest.getExtensionArchivo();

            tvpRqFiles.addRow(
                    indice,
                    rutaArchivo,
                    fileRequest.getNombreArchivo(),
                    fileRequest.getIdTipoArchivo(),
                    fileRequest.getIdTipoArchivoRQ());

            indice++;
        }
        return tvpRqFiles;
    }

    private static SQLServerDataTable loadTvpRequirementVacantes(List<VacanteRequirement> lstVacantes)
            throws SQLServerException {
        SQLServerDataTable tvpRqVacantes = new SQLServerDataTable();
        tvpRqVacantes.addColumnMetadata("ID_PERFIL", Types.INTEGER);
        tvpRqVacantes.addColumnMetadata("CANTIDAD", Types.INTEGER);
        tvpRqVacantes.addColumnMetadata("TARIFA_FINAL", Types.DECIMAL);

        for (VacanteRequirement vacanteRequirement : lstVacantes) {
            tvpRqVacantes.addRow(
                    vacanteRequirement.getIdPerfil(),
                    vacanteRequirement.getCantidad(),
                    vacanteRequirement.getTarifaFinal());
        }

        return tvpRqVacantes;
    }

    private static SQLServerDataTable loadTvpRequirementVacantesUpdate(List<VacanteRequirement> lstVacantes)
            throws SQLServerException {
        SQLServerDataTable tvpRqVacantes = new SQLServerDataTable();
        tvpRqVacantes.addColumnMetadata("ID_REQUERIMIENTO_VACANTE", Types.INTEGER);
        tvpRqVacantes.addColumnMetadata("ID_PERFIL", Types.INTEGER);
        tvpRqVacantes.addColumnMetadata("CANTIDAD", Types.INTEGER);
        tvpRqVacantes.addColumnMetadata("ID_ESTADO", Types.INTEGER);
        tvpRqVacantes.addColumnMetadata("TARIFA_FINAL", Types.DECIMAL);

        for (VacanteRequirement vacanteRequirement : lstVacantes) {
            tvpRqVacantes.addRow(
                    vacanteRequirement.getIdRequerimientoVacante(),
                    vacanteRequirement.getIdPerfil(),
                    vacanteRequirement.getCantidad(),
                    vacanteRequirement.getIdEstado(),
                    vacanteRequirement.getTarifaFinal());
        }

        return tvpRqVacantes;
    }

    @Async
    protected void guardarArchivos(List<FileRequest> lstFiles, Integer idNewRq, Integer idEmpresa) {
        for (FileRequest fileItem : lstFiles) {
            String rutaRq = Constante.RUTA_REPOSITORIO + idEmpresa
                    + Constante.RUTA_RQ_ARCHIVOS.replace("[ID_REQUERIMIENTO]", idNewRq.toString())
                    + fileItem.getNombreArchivo() + "." + fileItem.getExtensionArchivo();
            FileUtils.guardarArchivoAws(fileItem.getString64(), rutaRq);
        }
    }

    private static SQLServerDataTable loadTvpRequirementTalents(RequirementTalentRequest request)
            throws SQLServerException {
        SQLServerDataTable tvpRqTalents = new SQLServerDataTable();
        tvpRqTalents.addColumnMetadata("INDICE", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("ID_TALENTO", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("NOMBRES_TALENTO", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("APELLIDOS_TALENTO", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("DNI", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("CELULAR", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("EMAIL", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("ID_SITUACION", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("ID_ESTADO", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("ID_PERFIL", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("CONFIRMADO", Types.BIT);

        tvpRqTalents.addColumnMetadata("INGRESO", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("ID_CLIENTE", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("CLIENTE", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("ID_AREA", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("CARGO", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("FCH_INICIO_CONTRATO", Types.DATE);
        tvpRqTalents.addColumnMetadata("FCH_TERMINO_CONTRATO", Types.DATE);
        tvpRqTalents.addColumnMetadata("PROYECTO_SERVICIO", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("OBJETO_CONTRATO", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("ID_MODALIDAD_CONTRATO", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("HORARIO", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("TIENE_EQUIPO", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("UBICACION", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("ID_MOTIVO", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("ID_MONEDA", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("DECLARAR_SUNAT", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("SEDE_DECLARAR", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("MONTO_BASE", Types.DECIMAL);
        tvpRqTalents.addColumnMetadata("MONTO_MOVILIDAD", Types.DECIMAL);
        tvpRqTalents.addColumnMetadata("MONTO_MENSUAL", Types.DECIMAL);
        tvpRqTalents.addColumnMetadata("MONTO_TRIMESTRAL", Types.DECIMAL);
        tvpRqTalents.addColumnMetadata("MONTO_SEMESTRAL", Types.DECIMAL);

        // Solicitud Equipo
        tvpRqTalents.addColumnMetadata("AREA", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("FECHA_SOLICITUD", Types.DATE);
        tvpRqTalents.addColumnMetadata("FECHA_ENTREGA", Types.DATE);
        tvpRqTalents.addColumnMetadata("ID_TIPO_EQUIPO", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("TIPO_EQUIPO", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("PROCESADOR", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("RAM", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("HD", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("MARCA", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("ID_ANEXO", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("ANEXO", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("BIT_CELULAR", Types.BIT);
        tvpRqTalents.addColumnMetadata("BIT_INTERNET_MOVIL", Types.BIT);
        tvpRqTalents.addColumnMetadata("ACCESORIOS", Types.VARCHAR);

        // ID_ESTADO_REGISTRO PARA ACTUALIZACIONES
        tvpRqTalents.addColumnMetadata("ID_ESTADO_REGISTRO", Types.INTEGER);

        int indice = 1;

        System.out.println("CARGANDO DATOS A TABLA TVP");

        for (RequirementTalentRequestDTO talentRequest : request.getLstTalentos()) {
            SolicitudEquipoDTO se = talentRequest.getSolicitudEquipo();

            tvpRqTalents.addRow(
                    indice,
                    talentRequest.getIdTalento(),
                    talentRequest.getNombres(),
                    talentRequest.getApellidos(),
                    talentRequest.getDni(),
                    talentRequest.getCelular(),
                    talentRequest.getEmail(),
                    talentRequest.getIdSituacion(),
                    talentRequest.getIdEstado(),
                    talentRequest.getIdPerfil(),
                    talentRequest.isConfirmado() ? 1 : 0,

                    talentRequest.getIngreso() != null ? talentRequest.getIngreso() : 0,
                    talentRequest.getIdCliente(),
                    talentRequest.getCliente(),
                    talentRequest.getIdArea(),
                    talentRequest.getCargo(),
                    talentRequest.getFchInicioContrato(),
                    talentRequest.getFchTerminoContrato(),
                    talentRequest.getProyectoServicio(),
                    talentRequest.getObjetoContrato(),
                    talentRequest.getIdModalidadContrato(),
                    talentRequest.getHorario(),
                    talentRequest.getTieneEquipo() != null ? talentRequest.getTieneEquipo() : 0,
                    talentRequest.getUbicacion(),
                    talentRequest.getIdMotivo(),
                    talentRequest.getIdMoneda(),
                    talentRequest.getDeclararSunat() != null ? talentRequest.getDeclararSunat() : 0,
                    talentRequest.getSedeDeclarar(),
                    talentRequest.getMontoBase(),
                    talentRequest.getMontoMovilidad(),
                    talentRequest.getMontoMensual(),
                    talentRequest.getMontoTrimestral(),
                    talentRequest.getMontoSemestral(),

                    // Solicitud Equipo
                    talentRequest.getArea(),
                    se != null ? se.getFechaSolicitud() : null,
                    se != null ? se.getFechaEntrega() : null,
                    se != null ? se.getIdTipoEquipo() : null,
                    se != null ? se.getTipoEquipo() : null,
                    se != null ? se.getProcesador() : null,
                    se != null ? se.getRam() : null,
                    se != null ? se.getHd() : null,
                    se != null ? se.getMarca() : null,
                    se != null ? se.getIdAnexo() : null,
                    se != null ? se.getAnexo() : null,
                    se != null ? se.getBitCelular() : null,
                    se != null ? se.getBitInternetMovil() : null,
                    se != null ? se.getAccesorios() : null,
                    // ID_ESTADO_REGISTRO PARA ACTUALIZACIONES
                    talentRequest.getIdEstadoRegistro());

            indice++;
        }

        System.out.println("DATOS CARGADOS, RETORNANDO VALORES DE TVP");
        return tvpRqTalents;
    }

    private static SQLServerDataTable loadTvpProductos(RequirementTalentRequest request) throws SQLServerException {
        SQLServerDataTable tvpProductos = new SQLServerDataTable();
        tvpProductos.addColumnMetadata("ID_TALENTO", Types.INTEGER);
        tvpProductos.addColumnMetadata("ID_ITEM", Types.INTEGER);
        tvpProductos.addColumnMetadata("PRODUCTO", Types.VARCHAR);
        tvpProductos.addColumnMetadata("PROD_VERSION", Types.VARCHAR);

        for (RequirementTalentRequestDTO talent : request.getLstTalentos()) {
            SolicitudEquipoDTO se = talent.getSolicitudEquipo();

            if (se != null && se.getLstSoftware() != null) {
                for (SolicitudSoftwareRequest software : se.getLstSoftware()) {
                    tvpProductos.addRow(
                            talent.getIdTalento(),
                            software != null ? software.getIdItem() : null,
                            software != null ? software.getProducto() : null,
                            software != null ? software.getProdVersion() : null);
                }
            }
        }

        return tvpProductos;
    }

    private static PostulantDTO mapListPostulantDTO(Map<String, Object> postulanteRow) {
        return new PostulantDTO(
                (String) postulanteRow.get("NOMBRES"),
                (String) postulanteRow.get("APELLIDOS"),
                (String) postulanteRow.get("CELULAR"),
                (String) postulanteRow.get("EMAIL"),
                (String) postulanteRow.get("DNI"),
                (String) postulanteRow.get("TIEMPO_CONTRATO"),
                (String) postulanteRow.get("FCH_INICIO_LABORES"),
                (String) postulanteRow.get("CARGO"),
                (BigDecimal) postulanteRow.get("REMUNERACION"),
                (String) postulanteRow.get("MODALIDAD"),
                (String) postulanteRow.get("TIENE_EQUIPO"));
    }

    @Async
    public void updateRequirementAlertJob() {
        System.out.println("Ejecutando SP de alertas JOB");
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_REQUERIMIENTO_ALERTA_JOB");
        simpleJdbcCall.execute();
    }

    public FileResponse getRqFile(BaseRequest baseRequest, Integer idRequerimentFile) {

        SimpleJdbcCall sCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_REQUERIMIENTO_ARCHIVO_SEL");

        FileResponse fileResponse = new FileResponse();

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_REQUERIMIENTO_ARCHIVO", idRequerimentFile)
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("USUARIO", baseRequest.getUsername())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());

        Map<String, Object> resMap = sCall.execute(params);
        List<Map<String, Object>> resSeList = (List<Map<String, Object>>) resMap.get("#result-set-1");

        if (resSeList != null && !resSeList.isEmpty()) {

            Integer idTipoMensaje = (Integer) resSeList.get(0).get("ID_TIPO_MENSAJE");
            String mensaje = (String) resSeList.get(0).get("MENSAJE");
            fileResponse.setBaseResponse(new BaseResponse(
                    idTipoMensaje, mensaje));

            if (fileResponse.getBaseResponse().getIdTipoMensaje() == 2) {
                List<Map<String, Object>> fileSet = (List<Map<String, Object>>) resMap.get("#result-set-2");

                if (fileSet != null && !fileSet.isEmpty()) {
                    Map<String, Object> fileRow = fileSet.get(0);
                    String link = (String) fileRow.get("LINK");
                    // Extraer la extensión del archivo
                    String ext = "";
                    if (link != null && link.contains(".")) {
                        ext = link.substring(link.lastIndexOf('.') + 1);
                    }
                    fileResponse.setExt(ext);
                    fileResponse.setFile(link);

                }
            }
        }

        return fileResponse;
    }

    private static SQLServerDataTable loadTvpRqVacSkill(List<VacanteSkill> lstVacanteSkills)
            throws SQLServerException {
        SQLServerDataTable tvpRqVacSkill = new SQLServerDataTable();

        tvpRqVacSkill.addColumnMetadata("ID_PERFIL", Types.INTEGER);
        tvpRqVacSkill.addColumnMetadata("ID_SKILL", Types.INTEGER);
        tvpRqVacSkill.addColumnMetadata("ANIOS", Types.INTEGER);
        tvpRqVacSkill.addColumnMetadata("OPCIONAL", Types.INTEGER);

        // Recorrer la lista en Java y llenar el TVP
        for (VacanteSkill skillReq : lstVacanteSkills) {
            tvpRqVacSkill.addRow(
                    skillReq.getIdPerfil(),
                    skillReq.getIdSkill(),
                    skillReq.getAnios(),
                    skillReq.getIsOptional() ? 1 : 0);
        }

        return tvpRqVacSkill;
    }

    public VacanteSkillsResponse getTechSkillsForVac(Integer idVacante) {

        SimpleJdbcCall sCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_REQUERIMIENTO_VACANTE_HABILIDAD_SEL");

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_VACANTE", idVacante);

        List<VacanteSkillDTO> habilidades = new ArrayList<>();

        Map<String, Object> resMap = sCall.execute(params);

        List<Map<String, Object>> msgResult = (List<Map<String, Object>>) resMap.get("#result-set-1");
        Map<String, Object> msgRow = msgResult.get(0);
        Integer idTipoMensaje = (Integer) msgRow.get("ID_TIPO_MENSAJE");
        String mensaje = (String) msgRow.get("MENSAJE");

        if (idTipoMensaje == 2) {
            // Segundo resultset -> data
            List<Map<String, Object>> skills = (List<Map<String, Object>>) resMap.get("#result-set-2");
            for (Map<String, Object> row : skills) {
                Integer idSkill = (Integer) row.get("ID_HABILIDAD");
                String skillName = (String) row.get("HABILIDAD");
                Integer years = (Integer) row.get("ANIOS_EXP");
                Integer idRegState = (Integer) row.get("ID_ESTADO_REGISTRO");
                Integer idVac = (Integer) row.get("ID_VACANTE");
                Integer idProfile = (Integer) row.get("ID_PERFIL");
                Integer idReqVacSkill = (Integer) row.get("ID_VACANTE_HABILIDAD");
                Integer isOptional = (Integer) row.get("OPCIONAL");

                // Aquí mapear a DTO VacanteSkill
                VacanteSkillDTO skill = new VacanteSkillDTO();
                skill.setIdVacanteHabilidad(idReqVacSkill);
                skill.setIdVacante(idVac);
                skill.setIdPerfil(idProfile);
                skill.setIdHabilidad(idSkill);
                skill.setHabilidad(skillName);
                skill.setIdEstadoRegistro(idRegState);
                skill.setAnios(years);
                skill.setIsOptional(isOptional == 1);
                habilidades.add(skill);
            }
        }
        return new VacanteSkillsResponse(idTipoMensaje, mensaje, habilidades);
    }

    public BaseResponse updateSkillsForVac(BaseRequest baseRequest, Integer idVacante,
            List<VacanteSkillDTO> skills) {

        try {

            SimpleJdbcCall sCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("SP_REQUERIMIENTO_VACANTE_HABILIDAD_UPD");

            SQLServerDataTable tvp = new SQLServerDataTable();
            tvp.addColumnMetadata("ID_REQUERIMIENTO_VACANTE_HABILIDAD", java.sql.Types.INTEGER);
            tvp.addColumnMetadata("ID_HABILIDAD", java.sql.Types.INTEGER);
            tvp.addColumnMetadata("A_EXP", java.sql.Types.INTEGER);
            tvp.addColumnMetadata("ID_ESTADO_REGISTRO", java.sql.Types.INTEGER);
            tvp.addColumnMetadata("OPCIONAL", java.sql.Types.INTEGER);

            for (VacanteSkillDTO s : skills) {
                tvp.addRow(
                        s.getIdVacanteHabilidad(),
                        s.getIdHabilidad(),
                        s.getAnios(),
                        s.getIdEstadoRegistro(),
                        s.getIsOptional() ? 1 : 0);
            }
            SqlParameterSource params = new MapSqlParameterSource()
                    .addValue("ID_VACANTE", idVacante)
                    .addValue("USUARIO", baseRequest.getUsername())
                    .addValue("LST_HABILIDADES", tvp);

            Map<String, Object> resMap = sCall.execute(params);

            List<Map<String, Object>> msgResult = (List<Map<String, Object>>) resMap.get("#result-set-1");
            Map<String, Object> msgRow = msgResult.get(0);
            Integer idTipoMensaje = (Integer) msgRow.get("ID_TIPO_MENSAJE");
            String mensaje = (String) msgRow.get("MENSAJE");
            return new BaseResponse(idTipoMensaje, mensaje);
        } catch (Exception e) {
            return new BaseResponse(3, e.getMessage());
        }
    }

    public BaseResponse updateCareersForVac(Integer idVacante, BaseRequest request, List<VacanteCarreraDTO> careers) {
        BaseResponse baseResponse = new BaseResponse();
        try {

            SimpleJdbcCall sCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("SP_VACANTE_CARRERA_UPD");

            // Crear el TVP
            SQLServerDataTable tvp = new SQLServerDataTable();
            tvp.addColumnMetadata("ID_VACANTE_CARRERA", java.sql.Types.INTEGER);
            tvp.addColumnMetadata("CARRERA", java.sql.Types.VARCHAR);
            tvp.addColumnMetadata("ID_GRADO_ESTUDIOS", java.sql.Types.INTEGER);
            tvp.addColumnMetadata("ID_ESTADO_REGISTRO", java.sql.Types.INTEGER);
            tvp.addColumnMetadata("OPCIONAL", java.sql.Types.INTEGER);

            // Llenar el TVP con las carreras recibidas
            for (VacanteCarreraDTO carrera : careers) {
                tvp.addRow(
                        carrera.getIdVacanteCarrera(), // Puede ser null si es nueva
                        carrera.getCarrera(),
                        carrera.getIdGradoEstudios(),
                        carrera.getIdEstadoRegistro(),
                        carrera.getIsOptional() ? 1 : 0);
            }

            // Ejecutar el SP
            Map<String, Object> result = sCall.execute(
                    new MapSqlParameterSource()
                            .addValue("ID_VACANTE", idVacante)
                            .addValue("LST_CARRERAS", tvp)
                            .addValue("USUARIO", request.getUsername())
                            .addValue("ROL", request.getIdRol())
                            .addValue("FUNCIONALIDADES", request.getFuncionalidades()));

            // Procesar la respuesta (primer result set)
            List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");
            if (resultSet != null && !resultSet.isEmpty()) {
                Map<String, Object> row = resultSet.get(0);
                int tipoMensaje = (int) row.get("ID_TIPO_MENSAJE");
                String mensaje = (String) row.get("MENSAJE");
                baseResponse.setIdTipoMensaje(tipoMensaje);
                baseResponse.setMensaje(mensaje);
            } else {
                baseResponse.setIdTipoMensaje(3);
                baseResponse.setMensaje("Ha ocurrido un error al actualizar las carreras de la vacante");

            }
            System.out.println(baseResponse);
            return baseResponse;

        } catch (Exception e) {
            System.out.println(e);
            return new BaseResponse(3, "Ha ocurrido un error al actualizar las carreras de la vacante.");
        }
    }

    public VacanteCarreraResponse getCareersForVac(BaseRequest request, Integer idVac) {
        VacanteCarreraResponse response = new VacanteCarreraResponse(3, "Error inesperado.", null);

        try {
            SimpleJdbcCall sCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("SP_VACANTE_CARRERA_SEL");

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("ID_VACANTE", idVac)
                    .addValue("USUARIO", request.getUsername())
                    .addValue("ROL", request.getIdRol())
                    .addValue("FUNCIONALIDADES", request.getFuncionalidades());

            Map<String, Object> result = sCall.execute(params);

            // Procesar Result Set #1 (mensaje)
            List<Map<String, Object>> rs1 = (List<Map<String, Object>>) result.get("#result-set-1");
            if (rs1 == null || rs1.isEmpty()) {
                response.setIdTipoMensaje(3);
                response.setMensaje("No se obtuvó información de la vacante.");
                return response;
            }

            Map<String, Object> msgRow = rs1.get(0);
            Integer tipoMensaje = (Integer) msgRow.get("ID_TIPO_MENSAJE");
            String mensaje = (String) msgRow.get("MENSAJE");

            response.setIdTipoMensaje(tipoMensaje);
            response.setMensaje(mensaje);

            // Si hubo error, no continuar
            if (tipoMensaje == 1 || tipoMensaje == 3) {
                return response;
            }

            // Procesar Result Set #2 (lista de carreras)
            List<Map<String, Object>> rs2 = (List<Map<String, Object>>) result.get("#result-set-2");
            if (rs2 != null && !rs2.isEmpty()) {
                List<VacanteCarreraDTO> carreras = rs2.stream().map(row -> {
                    VacanteCarreraDTO dto = new VacanteCarreraDTO();
                    dto.setIdVacanteCarrera((Integer) row.get("ID_VACANTE_CARRERA"));
                    dto.setIdVacante((Integer) row.get("ID_VACANTE"));
                    dto.setCarrera((String) row.get("CARRERA"));
                    dto.setIdGradoEstudios((Integer) row.get("ID_GRADO_ESTUDIOS"));
                    dto.setIdEstadoRegistro((Integer) row.get("ID_ESTADO_REGISTRO"));

                    Integer isOptional = (Integer) row.get("OPCIONAL");
                    dto.setIsOptional(isOptional == 1);

                    return dto;
                }).toList();

                response.setCarreras(carreras);
            } else {
                response.setCarreras(List.of());
            }

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return new VacanteCarreraResponse(3, "Error al obtener las carreras de la vacante",
                    null);
        }
    }

}
