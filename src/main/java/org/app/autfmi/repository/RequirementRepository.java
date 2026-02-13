package org.app.autfmi.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.sqlserver.jdbc.SQLServerDataTable;
import com.microsoft.sqlserver.jdbc.SQLServerException;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.*;
import org.app.autfmi.model.report.RequirementReport;
import org.app.autfmi.model.report.RequirementReportMapper;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.FileResponse;
import org.app.autfmi.model.response.RequirementListResponse;
import org.app.autfmi.model.response.RequirementResponse;
import org.app.autfmi.model.response.TalentRequirementDataResponse;
import org.app.autfmi.model.response.VacanteCarreraResponse;
import org.app.autfmi.model.response.VacanteSkillsResponse;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.FileUtils;
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
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
@Repository
@RequiredArgsConstructor
public class RequirementRepository {

	@NonNull
	private final JdbcTemplate jdbcTemplate;
	private Logger logger = LoggerFactory.getLogger(RequirementRepository.class);

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

					// Bloque: Facturación

					List<RQFacturacionDTO> lstFacturacion = new ArrayList<>();

					var resultSet7 = (List<Map<String, Object>>) result.get("#result-set-7");
					if (resultSet7 != null && !resultSet7.isEmpty()) {

						for (var item : resultSet7) {

							var builder = RQFacturacionDTO.builder();

							builder.idRequerimiento((Integer) item.get("ID_REQUERIMIENTO"));
							builder.idRequerimientoFacturacion((Integer) item.get("ID_REQUERIMIENTO_FACTURACION"));
							builder.idEstadoRegistro((Integer) item.get("ID_ESTADO_REGISTRO"));
							builder.idModalidad((Integer) item.get("ID_MODALIDAD"));
							builder.idGrupoModalidad((Integer) item.get("ID_GRUPO_MODALIDAD"));
							builder.nombreModalidad((String) item.get("MODALIDAD"));

							builder.nombreGrupoModalidad((String) item.get("GRUPO_MODALIDAD"));
							builder.currencyType((Integer) item.get("ID_MONEDA"));

							builder.minBaseAmount((BigDecimal) item.get("MIN_MONTO_BASE"));
							builder.maxBaseAmount((BigDecimal) item.get("MAX_MONTO_BASE"));

							builder.minTravelAllowance((BigDecimal) item.get("MIN_MONTO_MOVILIDAD"));
							builder.maxTravelAllowance((BigDecimal) item.get("MAX_MONTO_MOVILIDAD"));

							builder.minMonthlyAmount((BigDecimal) item.get("MIN_MONTO_MENSUAL"));
							builder.maxMonthlyAmount((BigDecimal) item.get("MAX_MONTO_MENSUAL"));

							builder.minQuarterlyAmount((BigDecimal) item.get("MIN_MONTO_TRIMESTRAL"));
							builder.maxQuarterlyAmount((BigDecimal) item.get("MAX_MONTO_TRIMESTRAL"));

							builder.minSemiAnnualAmount((BigDecimal) item.get("MIN_MONTO_SEMESTRAL"));
							builder.maxSemiAnnualAmount((BigDecimal) item.get("MAX_MONTO_SEMESTRAL"));

							lstFacturacion.add(builder.build());
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
		var table = new SQLServerDataTable();

		table.addColumnMetadata("ID_REQUERIMIENTO_FACTURACION", Types.INTEGER);
		table.addColumnMetadata("ID_REQUERIMIENTO", Types.INTEGER);
		table.addColumnMetadata("ID_MODALIDAD", Types.INTEGER);
		table.addColumnMetadata("ID_GRUPO_MODALIDAD", Types.INTEGER);
		table.addColumnMetadata("ID_MONEDA", Types.INTEGER);
		table.addColumnMetadata("MIN_MONTO_BASE", Types.DECIMAL);
		table.addColumnMetadata("MAX_MONTO_BASE", Types.DECIMAL);
		table.addColumnMetadata("MIN_MONTO_MOVILIDAD", Types.DECIMAL);
		table.addColumnMetadata("MAX_MONTO_MOVILIDAD", Types.DECIMAL);
		table.addColumnMetadata("MIN_MONTO_MENSUAL", Types.DECIMAL);
		table.addColumnMetadata("MAX_MONTO_MENSUAL", Types.DECIMAL);
		table.addColumnMetadata("MIN_MONTO_TRIMESTRAL", Types.DECIMAL);
		table.addColumnMetadata("MAX_MONTO_TRIMESTRAL", Types.DECIMAL);
		table.addColumnMetadata("MIN_MONTO_SEMESTRAL", Types.DECIMAL);
		table.addColumnMetadata("MAX_MONTO_SEMESTRAL", Types.DECIMAL);
		table.addColumnMetadata("ID_ESTADO_REGISTRO", Types.INTEGER);

		if (lstFacturacion == null)
			return table;

		for (var item : lstFacturacion) {
			table.addRow(
					item.getIdRequerimientoFacturacion(),
					item.getIdRequerimiento(),
					item.getIdModalidad(),
					item.getIdGrupoModalidad(),
					item.getCurrencyType() != null ? item.getCurrencyType() : 3,

					item.getMinBaseAmount() != null ? item.getMinBaseAmount() : BigDecimal.ZERO,
					item.getMaxBaseAmount() != null ? item.getMaxBaseAmount() : BigDecimal.ZERO,
					item.getMinTravelAllowance() != null ? item.getMinTravelAllowance() : BigDecimal.ZERO,
					item.getMaxTravelAllowance() != null ? item.getMaxTravelAllowance() : BigDecimal.ZERO,
					item.getMinMonthlyAmount() != null ? item.getMinMonthlyAmount() : BigDecimal.ZERO,
					item.getMaxMonthlyAmount() != null ? item.getMaxMonthlyAmount() : BigDecimal.ZERO,
					item.getMinQuarterlyAmount() != null ? item.getMinQuarterlyAmount() : BigDecimal.ZERO,
					item.getMaxQuarterlyAmount() != null ? item.getMaxQuarterlyAmount() : BigDecimal.ZERO,
					item.getMinSemiAnnualAmount() != null ? item.getMinSemiAnnualAmount() : BigDecimal.ZERO,
					item.getMaxSemiAnnualAmount() != null ? item.getMaxSemiAnnualAmount() : BigDecimal.ZERO,

					item.getIdEstadoRegistro());
		}
		return table;
	}

	/*
	 * Crea o actualiza un requerimiento desde un agente externo
	 * N8N en Octubre 2025
	 */
	public BaseResponse saveRequirementByAgent(AgentRQRequest request, BaseRequest baseRequest)
			throws SQLServerException {
		String agentJson = null;

		ObjectMapper objectMapper = new ObjectMapper();
		try {
			agentJson = objectMapper.writeValueAsString(request);
		} catch (JsonProcessingException e) {
			return new BaseResponse(3, "Error al procesar datos del agente externo", e.getMessage());
		}

		logger.info("SaveRequirementByAgent started for: {}", request.getTitulo());
		SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
				.withProcedureName("SP_REQUERIMIENTO_INS_SMART");

		MapSqlParameterSource params = new MapSqlParameterSource()

				.addValue("ID_USUARIO", baseRequest.getIdUsuario())
				.addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
				.addValue("ID_ROL", baseRequest.getIdRol())
				.addValue("USUARIO", baseRequest.getUsername())
				.addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
				.addValue("CODIGO_RQ", request.getCodigoRQ())
				.addValue("AGENT_JSON", agentJson);

		Map<String, Object> result = simpleJdbcCall.execute(params);
		List<Map<String, Object>> rs1 = (List<Map<String, Object>>) result.get("#result-set-1");

		if (rs1 == null || rs1.isEmpty()) {
			return null;
		}

		Map<String, Object> row = rs1.get(0);
		Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
		String mensaje = (String) row.get("MENSAJE");

		return new BaseResponse(idTipoMensaje, mensaje);

	}

	public RequirementReport getRequirementReport(Integer idRequirement, Integer idUser)
			throws SQLServerException {

		SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
				.withProcedureName("SP_REQUERIMIENTO_REPORTE_SEL");

		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("ID_RQ", idRequirement)
				.addValue("ID_USUARIO", idUser);

		Map<String, Object> rs = simpleJdbcCall.execute(params);

		List<Map<String, Object>> rs1 = (List<Map<String, Object>>) rs.get("#result-set-1");
		List<Map<String, Object>> detailsResultSet = (List<Map<String, Object>>) rs.get("#result-set-2");
		List<Map<String, Object>> contactsResultSet = (List<Map<String, Object>>) rs.get("#result-set-3");
		List<Map<String, Object>> skillsResultSet = (List<Map<String, Object>>) rs.get("#result-set-4");
		List<Map<String, Object>> careersResultSet = (List<Map<String, Object>>) rs.get("#result-set-5");
		List<Map<String, Object>> postulantsResultSet = (List<Map<String, Object>>) rs.get("#result-set-6");
		List<Map<String, Object>> managersResultSet = (List<Map<String, Object>>) rs.get("#result-set-7");
		List<Map<String, Object>> actionUserResultSet = (List<Map<String, Object>>) rs.get("#result-set-8");
		List<Map<String, Object>> vacDetResultSet = (List<Map<String, Object>>) rs.get("#result-set-9");
		List<Map<String, Object>> extraMailSet = (List<Map<String, Object>>) rs.get("#result-set-10");

		if (rs1 == null || rs1.isEmpty()) {
			RequirementReport requirementReport = new RequirementReport();
			requirementReport.setResponse(new BaseResponse(3, "No se encontraron datos para el requerimiento"));
			return requirementReport;
		}

		Map<String, Object> row = rs1.get(0);
		Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
		String mensaje = (String) row.get("MENSAJE");
		BaseResponse response = new BaseResponse(idTipoMensaje, mensaje);

		if (idTipoMensaje != 2) {
			RequirementReport requirementReport = new RequirementReport();
			requirementReport.setResponse(response);
			return requirementReport;
		}

		// Mapear usando RequirementReportMapper
		RequirementReport requirementReport = RequirementReportMapper.mapCompleteReportV2(
				detailsResultSet != null && !detailsResultSet.isEmpty() ? detailsResultSet.get(0) : null,
				contactsResultSet,
				skillsResultSet,
				careersResultSet,
				postulantsResultSet,
				managersResultSet,
				actionUserResultSet != null && !actionUserResultSet.isEmpty() ? actionUserResultSet.get(0) : null,
				vacDetResultSet, extraMailSet);

		logger.info("RequirementReport mapeado exitosamente:");
		logger.info("- Detalles RQ: {}",
				requirementReport.getRequirementDetails() != null
						? requirementReport.getRequirementDetails().getCodigoRQ()
						: "No disponible");
		logger.info("- Contactos: {} registros", requirementReport.getContacts().size());
		logger.info("- Habilidades: {} registros", requirementReport.getVacanteSkills().size());
		logger.info("- Carreras: {} registros", requirementReport.getVacanteCareers().size());
		logger.info("- Postulantes: {} registros", requirementReport.getPostulants().size());
		logger.info("- Gestores: {} registros", requirementReport.getManagers().size());
		logger.info("- Usuario acción: {}",
				requirementReport.getActionUser() != null ? requirementReport.getActionUser().getUsuario()
						: "No disponible");

		requirementReport.setResponse(response);
		return requirementReport;

	}

	public BaseResponse saveRequirement(RequirementRequest request, BaseRequest baseRequest) throws SQLServerException {

		var simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_REQUERIMIENTO_INS");

		var tvpRqFiles = loadTvpRequirementFiles(request.getLstArchivos(), baseRequest.getIdEmpresa());
		var tvpRqVacantes = loadTvpRequirementVacantes(request.getLstVacantes());
		var tvpRqVacSkill = loadTvpRqVacSkill(request.getLstVacanteSkills());
		var tvpCarreras = loadTvpLstCarreras(request.getLstCarreras());
		var tvpFacturacion = loadLstFacturacionTable(request.getLstFacturacion());

		var params = new MapSqlParameterSource()
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

		var result = simpleJdbcCall.execute(params);
		var resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

		if (resultSet == null || resultSet.isEmpty()) {
			return new BaseResponse(3, "No se obtuvo respuesta de la base de datos");
		}

		Map<String, Object> row = resultSet.get(0);
		Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
		String mensaje = (String) row.get("MENSAJE");

		BaseResponse baseResponse = new BaseResponse(idTipoMensaje, mensaje);

		// Cargar arcchivo de RQ si se creó correctamente
		if (idTipoMensaje == 2) {
			guardarArchivos(request.getLstArchivos(), Integer.parseInt(mensaje), baseRequest.getIdEmpresa());
		}

		return baseResponse;
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

		var simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
				.withProcedureName("SP_REQUERIMIENTO_UPD");

		var tvpRqVacantes = loadTvpRequirementVacantesUpdate(request.getLstVacantes());

		var lstFactuacion = loadLstFacturacionTable(request.getLstFacturacion());

		var params = new MapSqlParameterSource()
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

		var result = simpleJdbcCall.execute(params);
		var resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

		if (resultSet == null || resultSet.isEmpty()) {
			return new BaseResponse(3, "No se obtuvo respuesta de la base de datos");
		}

		var row = resultSet.get(0);
		var idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
		var mensaje = (String) row.get("MENSAJE");
		return new BaseResponse(idTipoMensaje, mensaje);
	}

	public RequirementTalentsResult saveRequirementTalents(RequirementTalentRequest request, BaseRequest baseRequest)
			throws SQLServerException {

		var builder = RequirementTalentsResult.builder();
		logger.info("SaveRequirementTalents started - ID_REQUERIMIENTO: {}", request.getIdRequerimiento());

		// 1. Configuración y Ejecución del SP
		SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
				.withProcedureName("SP_REQUERIMIENTO_TALENTO_INS");

		var tvpRqTalents = loadTvpRequirementTalents(request);
		var tvpProductos = loadTvpProductos(request);

		var params = new MapSqlParameterSource()
				.addValue("ID_REQUERIMIENTO", request.getIdRequerimiento())
				.addValue("LST_TALENTOS", tvpRqTalents)
				.addValue("LST_SOFTWARE", tvpProductos)
				.addValue("ID_USUARIO", baseRequest.getIdUsuario())
				.addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
				.addValue("ID_ROL", baseRequest.getIdRol())
				.addValue("USUARIO", baseRequest.getUsername())
				.addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());

		this.logger.info("Executing stored procedure SP_REQUERIMIENTO_TALENTO_INS");
		Map<String, Object> result = simpleJdbcCall.execute(params);

		// 2. Validación de Respuesta Base
		var resultSet1 = (List<Map<String, Object>>) result.get("#result-set-1");

		if (resultSet1 == null || resultSet1.isEmpty()) {
			var bs = new BaseResponse(3, "No se obtuvo respuesta de la base de datos");
			builder.baseResponse(bs);
			return builder.build();
		}

		var baseResponseRow = resultSet1.get(0);
		var messageId = (Integer) baseResponseRow.getOrDefault("ID_TIPO_MENSAJE", 3);
		var mensaje = (String) baseResponseRow.get("MENSAJE");

		builder.baseResponse(new BaseResponse(messageId, mensaje));

		// Si hubo error, retornamos aquí
		if (messageId != 2) {
			return builder.build();
		}

		// 3. Extracción de ResultSets (Variables independientes para legibilidad)
		var postulantsSet = (List<Map<String, Object>>) result.get("#result-set-2");
		var notificacionesCc = (List<Map<String, Object>>) result.get("#result-set-3");
		var entryReportsRs = (List<Map<String, Object>>) result.get("#result-set-4");
		var solicitudesEquipoRs = (List<Map<String, Object>>) result.get("#result-set-5");
		var usuarioActuador = (List<Map<String, Object>>) result.get("#result-set-6");

		// 4. Mapeo de Postulantes
		if (postulantsSet != null && !postulantsSet.isEmpty()) {
			List<PostulantDTO> postulantList = new ArrayList<>();
			for (var row : postulantsSet) {
				postulantList.add(mapListPostulantDTO(row));
			}
			builder.postulantes(postulantList);
		}

		// 5. Mapeo de Notificaciones
		List<String> lstCc = new ArrayList<>();
		if (notificacionesCc != null && !notificacionesCc.isEmpty()) {
			notificacionesCc.stream().forEach((n) -> {
				var correo = (String) n.getOrDefault("CORREO", "");
				lstCc.add(correo);
			});
		}

		builder.ccList(lstCc);

		// 6. Mapeo de Reportes de Ingreso
		if (entryReportsRs != null && !entryReportsRs.isEmpty()) {
			List<RequirementTalentsResult.ReporteIngreso> reportesIngreso = new ArrayList<>();

			entryReportsRs.stream().forEach((report) -> {
				var reporte = new RequirementTalentsResult.ReporteIngreso();
				reporte.setIdHistorial((Integer) report.get("ID_HISTORIAL"));
				reporte.setIdTalento((Integer) report.get("ID_TALENTO"));
				reporte.setIdTipoHistorial((Integer) report.get("ID_TIPO_HISTORIAL"));
				reportesIngreso.add(reporte);
			});

			builder.reportesIngreso(reportesIngreso);
		}

		// 7. Mapeo de Reportes de Solicitud de Equipo
		if (solicitudesEquipoRs != null && !solicitudesEquipoRs.isEmpty()) {
			List<RequirementTalentsResult.ReporteSolicitudEquipo> reportesSolicitudEquipo = new ArrayList<>();
			solicitudesEquipoRs.stream().forEach((report) -> {
				var reporte = new RequirementTalentsResult.ReporteSolicitudEquipo();
				reporte.setIdSolicitudEquipo((Integer) report.get("ID_SOLICITUD_EQUIPO"));
				reporte.setIdTalento((Integer) report.get("ID_TALENTO"));
				reportesSolicitudEquipo.add(reporte);
			});
			builder.reportesSolicitudEquipo(reportesSolicitudEquipo);
		}

		// 8. Mapeao del usuario actuador conocido como (GestorRQ, no siempre lo es)
		if (usuarioActuador != null && !usuarioActuador.isEmpty()) {
			var usuario = usuarioActuador.get(0);
			var gestorRq = new GestorRqDTO();
			gestorRq.setNombres((String) usuario.get("NOMBRES"));
			gestorRq.setApellidos((String) usuario.get("APELLIDOS"));
			gestorRq.setCorreo((String) usuario.get("CORREO"));
			gestorRq.setCodigoRQ((String) usuario.get("CODIGO_RQ"));

			builder.gestorRq(gestorRq);
		}

		logger.info("Finished task: saveRequirementTalents");
		return builder.build();

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
				(String) postulanteRow.get("REMUNERACION"),
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
