package org.app.autfmi.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalClientDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalDecisionDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalDetailDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalFlowDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalGestorDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalInboxDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalItemDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalOutcome;
import org.app.autfmi.model.dto.aprobacion.ApprovalRegistrationDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalResult;
import org.app.autfmi.model.dto.aprobacion.ApprovalTimelineEntryDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovedRequestDTO;
import org.app.autfmi.model.request.ApprovalListRequest;
import org.app.autfmi.model.response.PaginatedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class ApprovalRepository {

  private static final String MENSAJE_ERROR = "Error al consultar la bandeja de aprobaciones";
  private static final String MENSAJE_ERROR_FLUJO = "Error al procesar la solicitud de aprobación";

  private final JdbcTemplate jdbcTemplate;
  private final Logger logger = LoggerFactory.getLogger(ApprovalRepository.class);

  public ApprovalResult<ApprovalInboxDTO> listRequests(ApprovalListRequest request, UserDTO user, String idRoles) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_FMI_SOLICITUD_APROBACION_LST");
    try {
      MapSqlParameterSource params = identityParams(user, idRoles)
          .addValue("ID_CLIENTE", request.getIdCliente())
          .addValue("TIPO_SOLICITUD", request.getTipoSolicitud())
          .addValue("ESTADO", request.getEstado())
          .addValue("BUSQUEDA", request.getBusqueda())
          .addValue("NUM_PAG", request.getNPag())
          .addValue("CANT_PAG", request.getCPag());
      return mapInbox(call.execute(params), request.getNPag());
    } catch (Exception e) {
      this.logger.error("Error en listRequests: ", e);
      return ApprovalResult.error(MENSAJE_ERROR);
    }
  }

  public ApprovalResult<ApprovalDetailDTO> getRequest(Integer idSolicitud, UserDTO user, String idRoles) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_FMI_SOLICITUD_APROBACION_SEL");
    try {
      MapSqlParameterSource params = identityParams(user, idRoles)
          .addValue("ID_SOLICITUD", idSolicitud);
      return mapDetail(call.execute(params));
    } catch (Exception e) {
      this.logger.error("Error en getRequest: ", e);
      return ApprovalResult.error(MENSAJE_ERROR);
    }
  }

  // ---------------------------------------------------------------- FTBO-1124

  public ApprovalResult<ApprovalRegistrationDTO> register(String tipoSolicitud, Integer idCliente, Integer idTalento,
      String nombreTalento, String resumen, String datosFormulario, UserDTO user, String idRoles,
      String funcionalidades) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_FMI_SOLICITUD_APROBACION_REG");
    try {
      MapSqlParameterSource params = new MapSqlParameterSource()
          .addValue("TIPO_SOLICITUD", tipoSolicitud)
          .addValue("ID_CLIENTE", idCliente)
          .addValue("ID_TALENTO", idTalento)
          .addValue("NOMBRE_TALENTO", nombreTalento)
          .addValue("RESUMEN", resumen)
          .addValue("DATOS_FORMULARIO", datosFormulario)
          .addValue("ID_USUARIO", user.getIdUsuario())
          .addValue("ID_EMPRESA", user.getIdEmpresa())
          .addValue("ID_ROLES", idRoles)
          .addValue("ID_FUNCIONALIDADES", funcionalidades)
          .addValue("USUARIO", user.getUsuario());
      return mapRegistration(call.execute(params));
    } catch (Exception e) {
      this.logger.error("Error en register: ", e);
      return ApprovalResult.error(MENSAJE_ERROR_FLUJO);
    }
  }

  public ApprovalResult<ApprovalFlowDTO> getFlow(Integer idSolicitud, UserDTO user, String idRoles) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_FMI_SOLICITUD_APROBACION_FLUJO_SEL");
    try {
      MapSqlParameterSource params = identityParams(user, idRoles)
          .addValue("ID_SOLICITUD", idSolicitud);
      return mapFlow(call.execute(params));
    } catch (Exception e) {
      this.logger.error("Error en getFlow: ", e);
      return ApprovalResult.error(MENSAJE_ERROR);
    }
  }

  public ApprovalResult<ApprovalDecisionDTO> updateForm(Integer idSolicitud, Long version, String datosFormulario,
      String detalle, UserDTO user, String idRoles) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_FMI_SOLICITUD_APROBACION_UPD");
    try {
      MapSqlParameterSource params = identityParams(user, idRoles)
          .addValue("ID_SOLICITUD", idSolicitud)
          .addValue("VERSION", version)
          .addValue("DATOS_FORMULARIO", datosFormulario)
          .addValue("NOMBRE_TALENTO", null)
          .addValue("DETALLE", detalle);
      Map<String, Object> status = firstRow(call.execute(params), "#result-set-1");
      if (status == null) {
        return ApprovalResult.error(MENSAJE_ERROR_FLUJO);
      }
      ApprovalOutcome outcome = ApprovalOutcome.fromCode(status.get("CODIGO_RESULTADO"));
      if (outcome != ApprovalOutcome.OK) {
        return statusOnly(outcome, status, MENSAJE_ERROR_FLUJO);
      }
      ApprovalDecisionDTO data = new ApprovalDecisionDTO(idSolicitud, "PENDIENTE", toLong(status.get("VERSION")));
      return ApprovalResult.of(outcome, 2, (String) status.get("MENSAJE"), data);
    } catch (Exception e) {
      this.logger.error("Error en updateForm: ", e);
      return ApprovalResult.error(MENSAJE_ERROR_FLUJO);
    }
  }

  public ApprovalResult<ApprovedRequestDTO> approve(Integer idSolicitud, Long version, UserDTO user, String idRoles) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_FMI_SOLICITUD_APROBACION_APR");
    try {
      MapSqlParameterSource params = identityParams(user, idRoles)
          .addValue("ID_SOLICITUD", idSolicitud)
          .addValue("VERSION", version);
      return mapApproved(call.execute(params));
    } catch (Exception e) {
      this.logger.error("Error en approve: ", e);
      return ApprovalResult.error(MENSAJE_ERROR_FLUJO);
    }
  }

  /**
   * Resultado de ejecutar la operación aprobada: true registrada, false
   * rechazada por su SP (vuelve a PENDIENTE), null incierto (sigue APROBADA).
   * Devuelve false si el SP no lo aceptó o falló.
   */
  public boolean registerResult(Integer idSolicitud, Boolean exito, String detalle, UserDTO user) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_FMI_SOLICITUD_APROBACION_RES");
    try {
      MapSqlParameterSource params = new MapSqlParameterSource()
          .addValue("ID_SOLICITUD", idSolicitud)
          .addValue("EXITO", exito)
          .addValue("DETALLE", truncate(detalle, 2000))
          .addValue("ID_USUARIO", user.getIdUsuario())
          .addValue("USUARIO", user.getUsuario());
      Map<String, Object> status = firstRow(call.execute(params), "#result-set-1");
      boolean ok = status != null && ApprovalOutcome.fromCode(status.get("CODIGO_RESULTADO")) == ApprovalOutcome.OK;
      if (!ok) {
        this.logger.error("No se registró el resultado de la solicitud {}: {}", idSolicitud,
            status != null ? status.get("MENSAJE") : "sin respuesta");
      }
      return ok;
    } catch (Exception e) {
      this.logger.error("Error en registerResult: ", e);
      return false;
    }
  }

  /** Reserva el envío de un correo; true solo para un llamador (evita duplicados). */
  public boolean reserveMail(Integer idSolicitud, String tipoCorreo, String usuario, Integer idUsuario) {
    Map<String, Object> status = mailRegistry(idSolicitud, tipoCorreo, "RESERVAR", null, null, usuario, idUsuario);
    return status != null
        && ApprovalOutcome.fromCode(status.get("CODIGO_RESULTADO")) == ApprovalOutcome.OK
        && Boolean.TRUE.equals(status.get("RESERVADO"));
  }

  /** Cierra la reserva con ENVIADO | OMITIDO | FALLIDO y lo anota en la línea de tiempo. */
  public void mailResult(Integer idSolicitud, String tipoCorreo, String estadoEnvio, String detalle,
      String usuario, Integer idUsuario) {
    mailRegistry(idSolicitud, tipoCorreo, "RESULTADO", estadoEnvio, truncate(detalle, 1000), usuario, idUsuario);
  }

  private Map<String, Object> mailRegistry(Integer idSolicitud, String tipoCorreo, String modo, String estadoEnvio,
      String detalle, String usuario, Integer idUsuario) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_FMI_SOLICITUD_APROBACION_CORREO_REG");
    try {
      MapSqlParameterSource params = new MapSqlParameterSource()
          .addValue("ID_SOLICITUD", idSolicitud)
          .addValue("TIPO_CORREO", tipoCorreo)
          .addValue("MODO", modo)
          .addValue("ESTADO_ENVIO", estadoEnvio)
          .addValue("DETALLE", detalle)
          .addValue("ID_USUARIO", idUsuario)
          .addValue("USUARIO", usuario);
      Map<String, Object> status = firstRow(call.execute(params), "#result-set-1");
      if (status == null || ApprovalOutcome.fromCode(status.get("CODIGO_RESULTADO")) != ApprovalOutcome.OK) {
        this.logger.error("Registro de correo {} {} de la solicitud {} rechazado: {}", tipoCorreo, modo, idSolicitud,
            status != null ? status.get("MENSAJE") : "sin respuesta");
      }
      return status;
    } catch (Exception e) {
      this.logger.error("Error en mailRegistry: ", e);
      return null;
    }
  }

  static ApprovalResult<ApprovalRegistrationDTO> mapRegistration(Map<String, Object> result) {
    Map<String, Object> status = firstRow(result, "#result-set-1");
    if (status == null) {
      return ApprovalResult.error(MENSAJE_ERROR_FLUJO);
    }
    ApprovalOutcome outcome = ApprovalOutcome.fromCode(status.get("CODIGO_RESULTADO"));
    if (outcome != ApprovalOutcome.OK) {
      return statusOnly(outcome, status, MENSAJE_ERROR_FLUJO);
    }
    List<ApprovalGestorDTO> gestores = new ArrayList<>();
    List<Map<String, Object>> rs2 = (List<Map<String, Object>>) result.get("#result-set-2");
    if (rs2 != null) {
      for (Map<String, Object> row : rs2) {
        gestores.add(new ApprovalGestorDTO(toInteger(row.get("ID_USUARIO")), (String) row.get("USUARIO"),
            toInteger(row.get("ID_TIPO_ROL"))));
      }
    }
    ApprovalRegistrationDTO data = new ApprovalRegistrationDTO(toInteger(status.get("ID_SOLICITUD")),
        (String) status.get("CLIENTE"), gestores);
    return ApprovalResult.of(outcome, 2, (String) status.get("MENSAJE"), data);
  }

  static ApprovalResult<ApprovalFlowDTO> mapFlow(Map<String, Object> result) {
    Map<String, Object> status = firstRow(result, "#result-set-1");
    if (status == null) {
      return ApprovalResult.error(MENSAJE_ERROR);
    }
    ApprovalOutcome outcome = ApprovalOutcome.fromCode(status.get("CODIGO_RESULTADO"));
    if (outcome != ApprovalOutcome.OK) {
      return statusOnly(outcome, status);
    }
    Map<String, Object> row = firstRow(result, "#result-set-2");
    if (row == null) {
      return ApprovalResult.of(ApprovalOutcome.NO_ENCONTRADO, 1, "La solicitud no existe o no está asignada a sus clientes.", null);
    }
    List<ApprovalTimelineEntryDTO> historial = new ArrayList<>();
    List<Map<String, Object>> rs3 = (List<Map<String, Object>>) result.get("#result-set-3");
    if (rs3 != null) {
      for (Map<String, Object> h : rs3) {
        historial.add(new ApprovalTimelineEntryDTO(
            toInteger(h.get("ID_HISTORIAL")),
            (String) h.get("ACCION"),
            (String) h.get("ESTADO_ANTERIOR"),
            (String) h.get("ESTADO_NUEVO"),
            (String) h.get("DETALLE"),
            (String) h.get("USUARIO"),
            (String) h.get("FECHA")));
      }
    }
    ApprovalFlowDTO flow = new ApprovalFlowDTO(
        (String) row.get("TIPO_SOLICITUD"),
        (String) row.get("ESTADO"),
        Boolean.TRUE.equals(row.get("ES_GESTOR")),
        Boolean.TRUE.equals(row.get("PUEDE_GESTIONAR")),
        toLong(row.get("VERSION")),
        List.of(),
        historial);
    return ApprovalResult.of(outcome, 2, (String) status.get("MENSAJE"), flow);
  }

  static ApprovalResult<ApprovedRequestDTO> mapApproved(Map<String, Object> result) {
    Map<String, Object> status = firstRow(result, "#result-set-1");
    if (status == null) {
      return ApprovalResult.error(MENSAJE_ERROR_FLUJO);
    }
    ApprovalOutcome outcome = ApprovalOutcome.fromCode(status.get("CODIGO_RESULTADO"));
    if (outcome != ApprovalOutcome.OK) {
      return statusOnly(outcome, status, MENSAJE_ERROR_FLUJO);
    }
    Map<String, Object> row = firstRow(result, "#result-set-2");
    if (row == null) {
      return ApprovalResult.error(MENSAJE_ERROR_FLUJO);
    }
    ApprovedRequestDTO data = new ApprovedRequestDTO(
        toInteger(row.get("ID_SOLICITUD")),
        (String) row.get("TIPO_SOLICITUD"),
        toInteger(row.get("ID_CLIENTE")),
        toInteger(row.get("ID_TALENTO")),
        (String) row.get("NOMBRE_TALENTO"),
        (String) row.get("DATOS_FORMULARIO"));
    return ApprovalResult.of(outcome, 2, (String) status.get("MENSAJE"), data);
  }

  private static Map<String, Object> firstRow(Map<String, Object> result, String key) {
    List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get(key);
    return rows == null || rows.isEmpty() ? null : rows.get(0);
  }

  private static Long toLong(Object value) {
    return value instanceof Number number ? number.longValue() : null;
  }

  private static String truncate(String value, int max) {
    return value == null || value.length() <= max ? value : value.substring(0, max);
  }

  private MapSqlParameterSource identityParams(UserDTO user, String idRoles) {
    return new MapSqlParameterSource()
        .addValue("ID_USUARIO", user.getIdUsuario())
        .addValue("ID_EMPRESA", user.getIdEmpresa())
        .addValue("ID_ROLES", idRoles)
        .addValue("USUARIO", user.getUsuario());
  }

  static ApprovalResult<ApprovalInboxDTO> mapInbox(Map<String, Object> result, Integer requestedPage) {
    List<Map<String, Object>> rs1 = (List<Map<String, Object>>) result.get("#result-set-1");
    if (rs1 == null || rs1.isEmpty()) {
      return ApprovalResult.error(MENSAJE_ERROR);
    }
    Map<String, Object> status = rs1.get(0);
    ApprovalOutcome outcome = ApprovalOutcome.fromCode(status.get("CODIGO_RESULTADO"));
    if (outcome != ApprovalOutcome.OK) {
      return statusOnly(outcome, status);
    }

    List<ApprovalItemDTO> items = new ArrayList<>();
    List<Map<String, Object>> rs2 = (List<Map<String, Object>>) result.get("#result-set-2");
    if (rs2 != null) {
      for (Map<String, Object> row : rs2) {
        items.add(ApprovalItemDTO.builder()
            .idSolicitud(toInteger(row.get("ID_SOLICITUD")))
            .tipoSolicitud((String) row.get("TIPO_SOLICITUD"))
            .estado((String) row.get("ESTADO"))
            .idCliente(toInteger(row.get("ID_CLIENTE")))
            .cliente((String) row.get("CLIENTE"))
            .idTalento(toInteger(row.get("ID_TALENTO")))
            .nombreTalento((String) row.get("NOMBRE_TALENTO"))
            .resumen((String) row.get("RESUMEN"))
            .usuarioSolicitante((String) row.get("USUARIO_SOLICITANTE"))
            .fechaSolicitud((String) row.get("FECHA_SOLICITUD"))
            .build());
      }
    }

    List<ApprovalClientDTO> clientes = new ArrayList<>();
    List<Map<String, Object>> rs3 = (List<Map<String, Object>>) result.get("#result-set-3");
    if (rs3 != null) {
      for (Map<String, Object> row : rs3) {
        clientes.add(new ApprovalClientDTO(toInteger(row.get("ID_CLIENTE")), (String) row.get("RAZON_SOCIAL")));
      }
    }

    PaginatedResponse<ApprovalItemDTO> page = PaginatedResponse.<ApprovalItemDTO>builder()
        .items(items)
        .totalElements(toInt(status.get("TOTAL_ELEMENTOS")))
        .totalPages(toInt(status.get("TOTAL_PAGINAS")))
        .currentPage(requestedPage != null && requestedPage > 0 ? requestedPage : 1)
        .build();
    return ApprovalResult.of(outcome, 2, (String) status.get("MENSAJE"), new ApprovalInboxDTO(page, clientes));
  }

  static ApprovalResult<ApprovalDetailDTO> mapDetail(Map<String, Object> result) {
    List<Map<String, Object>> rs1 = (List<Map<String, Object>>) result.get("#result-set-1");
    if (rs1 == null || rs1.isEmpty()) {
      return ApprovalResult.error(MENSAJE_ERROR);
    }
    Map<String, Object> status = rs1.get(0);
    ApprovalOutcome outcome = ApprovalOutcome.fromCode(status.get("CODIGO_RESULTADO"));
    if (outcome != ApprovalOutcome.OK) {
      return statusOnly(outcome, status);
    }

    List<Map<String, Object>> rs2 = (List<Map<String, Object>>) result.get("#result-set-2");
    if (rs2 == null || rs2.isEmpty()) {
      return ApprovalResult.of(ApprovalOutcome.NO_ENCONTRADO, 1, "La solicitud no existe o no está asignada a sus clientes.", null);
    }
    Map<String, Object> row = rs2.get(0);
    ApprovalDetailDTO detail = ApprovalDetailDTO.builder()
        .idSolicitud(toInteger(row.get("ID_SOLICITUD")))
        .tipoSolicitud((String) row.get("TIPO_SOLICITUD"))
        .estado((String) row.get("ESTADO"))
        .idCliente(toInteger(row.get("ID_CLIENTE")))
        .cliente((String) row.get("CLIENTE"))
        .idTalento(toInteger(row.get("ID_TALENTO")))
        .nombreTalento((String) row.get("NOMBRE_TALENTO"))
        .resumen((String) row.get("RESUMEN"))
        .datosFormulario((String) row.get("DATOS_FORMULARIO"))
        .usuarioSolicitante((String) row.get("USUARIO_SOLICITANTE"))
        .fechaSolicitud((String) row.get("FECHA_SOLICITUD"))
        .usuarioModificacion((String) row.get("USUARIO_MODIFICACION"))
        .fechaModificacion((String) row.get("FECHA_MODIFICACION"))
        .build();
    return ApprovalResult.of(outcome, 2, (String) status.get("MENSAJE"), detail);
  }

  /**
   * Respuesta sin datos. El texto de un error de base de datos no se devuelve
   * al cliente: queda en el log.
   */
  private static <T> ApprovalResult<T> statusOnly(ApprovalOutcome outcome, Map<String, Object> status) {
    return statusOnly(outcome, status, MENSAJE_ERROR);
  }

  private static <T> ApprovalResult<T> statusOnly(ApprovalOutcome outcome, Map<String, Object> status,
      String mensajeError) {
    if (outcome == ApprovalOutcome.ERROR) {
      LoggerFactory.getLogger(ApprovalRepository.class).error("SP de la bandeja respondió error: {}", status.get("MENSAJE"));
      return ApprovalResult.error(mensajeError);
    }
    return ApprovalResult.of(outcome, toInt(status.get("ID_TIPO_MENSAJE")), (String) status.get("MENSAJE"), null);
  }

  private static Integer toInteger(Object value) {
    return value instanceof Number number ? number.intValue() : null;
  }

  private static int toInt(Object value) {
    return value instanceof Number number ? number.intValue() : 0;
  }
}
