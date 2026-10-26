package org.app.autfmi.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalClientDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalDetailDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalInboxDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalItemDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalOutcome;
import org.app.autfmi.model.dto.aprobacion.ApprovalResult;
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
    if (outcome == ApprovalOutcome.ERROR) {
      LoggerFactory.getLogger(ApprovalRepository.class).error("SP de la bandeja respondió error: {}", status.get("MENSAJE"));
      return ApprovalResult.error(MENSAJE_ERROR);
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
