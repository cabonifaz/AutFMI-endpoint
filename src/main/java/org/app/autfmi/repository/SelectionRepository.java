package org.app.autfmi.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.seleccion.SelectionInterviewsDTO;
import org.app.autfmi.model.dto.seleccion.SelectionLabelCountDTO;
import org.app.autfmi.model.dto.seleccion.SelectionPerformanceRowDTO;
import org.app.autfmi.model.dto.seleccion.SelectionSeriePointDTO;
import org.app.autfmi.model.dto.seleccion.SelectionSummaryDTO;
import org.app.autfmi.model.dto.seleccion.SelectionUserDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.SelectionFilterRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.OperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Acceso a datos de las estadísticas del módulo Selección. Toda la agregación se
 * hace en los SP (SP_BT_SELECCION_*); aquí solo se ejecutan y se mapean los
 * result-sets. Cada SP devuelve en #result-set-1 el mensaje de estado.
 */
@Repository
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class SelectionRepository {

  private final JdbcTemplate jdbcTemplate;
  private final Logger logger = LoggerFactory.getLogger(SelectionRepository.class);

  public OperationResult<SelectionSummaryDTO> getResumen(SelectionFilterRequest request, BaseRequest baseRequest) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_BT_SELECCION_RESUMEN");
    try {
      Map<String, Object> result = call.execute(baseParams(request, baseRequest, false));

      BaseResponse status = readStatus(result);
      if (status.getIdTipoMensaje() != 2) {
        return new OperationResult<>(status, null);
      }

      List<Map<String, Object>> rs2 = (List<Map<String, Object>>) result.get("#result-set-2");
      SelectionSummaryDTO dto = new SelectionSummaryDTO(0, 0);
      if (rs2 != null && !rs2.isEmpty()) {
        Map<String, Object> row = rs2.get(0);
        dto.setTotalEntrevistas(toInt(row.get("TOTAL_ENTREVISTAS")));
        dto.setTotalIngresos(toInt(row.get("TOTAL_INGRESOS")));
      }
      return new OperationResult<>(status, dto);
    } catch (Exception e) {
      this.logger.error("Error en getResumen: ", e);
      return new OperationResult<>(new BaseResponse(3, "Error al obtener el resumen"), null);
    }
  }

  public OperationResult<SelectionInterviewsDTO> getEntrevistas(SelectionFilterRequest request, BaseRequest baseRequest) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_BT_SELECCION_ENTREVISTAS");
    try {
      MapSqlParameterSource params = baseParams(request, baseRequest, true)
          .addValue("USUCRE", request.getUsucre());
      Map<String, Object> result = call.execute(params);

      BaseResponse status = readStatus(result);
      if (status.getIdTipoMensaje() != 2) {
        return new OperationResult<>(status, null);
      }

      int total = 0;
      List<Map<String, Object>> rs2 = (List<Map<String, Object>>) result.get("#result-set-2");
      if (rs2 != null && !rs2.isEmpty()) {
        total = toInt(rs2.get(0).get("TOTAL_ENTREVISTAS"));
      }

      List<SelectionSeriePointDTO> serie = new ArrayList<>();
      List<Map<String, Object>> rs3 = (List<Map<String, Object>>) result.get("#result-set-3");
      if (rs3 != null) {
        for (Map<String, Object> row : rs3) {
          serie.add(new SelectionSeriePointDTO((String) row.get("PERIODO"), toInt(row.get("CANTIDAD"))));
        }
      }

      List<SelectionLabelCountDTO> porUsuario = new ArrayList<>();
      List<Map<String, Object>> rs4 = (List<Map<String, Object>>) result.get("#result-set-4");
      if (rs4 != null) {
        for (Map<String, Object> row : rs4) {
          porUsuario.add(new SelectionLabelCountDTO(null, (String) row.get("USUARIO"), toInt(row.get("CANTIDAD"))));
        }
      }

      int usuarioTotal = 0;
      List<Map<String, Object>> rs5 = (List<Map<String, Object>>) result.get("#result-set-5");
      if (rs5 != null && !rs5.isEmpty()) {
        usuarioTotal = toInt(rs5.get(0).get("TOTAL_USUARIO"));
      }

      List<SelectionSeriePointDTO> usuarioSerie = new ArrayList<>();
      List<Map<String, Object>> rs6 = (List<Map<String, Object>>) result.get("#result-set-6");
      if (rs6 != null) {
        for (Map<String, Object> row : rs6) {
          usuarioSerie.add(new SelectionSeriePointDTO((String) row.get("PERIODO"), toInt(row.get("CANTIDAD"))));
        }
      }

      return new OperationResult<>(status,
          new SelectionInterviewsDTO(total, serie, porUsuario, usuarioTotal, usuarioSerie));
    } catch (Exception e) {
      this.logger.error("Error en getEntrevistas: ", e);
      return new OperationResult<>(new BaseResponse(3, "Error al obtener las entrevistas"), null);
    }
  }

  public OperationResult<List<SelectionLabelCountDTO>> getIngresos(SelectionFilterRequest request, BaseRequest baseRequest) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_BT_SELECCION_INGRESOS");
    try {
      Map<String, Object> result = call.execute(baseParams(request, baseRequest, true));

      BaseResponse status = readStatus(result);
      if (status.getIdTipoMensaje() != 2) {
        return new OperationResult<>(status, null);
      }

      List<SelectionLabelCountDTO> list = new ArrayList<>();
      List<Map<String, Object>> rs2 = (List<Map<String, Object>>) result.get("#result-set-2");
      if (rs2 != null) {
        for (Map<String, Object> row : rs2) {
          Object id = row.get("ID_CLIENTE");
          list.add(new SelectionLabelCountDTO(id == null ? null : toInt(id),
              (String) row.get("CLIENTE"), toInt(row.get("CANTIDAD"))));
        }
      }
      return new OperationResult<>(status, list);
    } catch (Exception e) {
      this.logger.error("Error en getIngresos: ", e);
      return new OperationResult<>(new BaseResponse(3, "Error al obtener los ingresos"), null);
    }
  }

  public OperationResult<List<SelectionPerformanceRowDTO>> getRendimiento(SelectionFilterRequest request, BaseRequest baseRequest) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_BT_SELECCION_RENDIMIENTO");
    try {
      Map<String, Object> result = call.execute(baseParams(request, baseRequest, true));

      BaseResponse status = readStatus(result);
      if (status.getIdTipoMensaje() != 2) {
        return new OperationResult<>(status, null);
      }

      List<SelectionPerformanceRowDTO> list = new ArrayList<>();
      List<Map<String, Object>> rs2 = (List<Map<String, Object>>) result.get("#result-set-2");
      if (rs2 != null) {
        for (Map<String, Object> row : rs2) {
          list.add(new SelectionPerformanceRowDTO((String) row.get("CLIENTE"),
              toInt(row.get("ENTREVISTAS")), toInt(row.get("INGRESOS"))));
        }
      }
      return new OperationResult<>(status, list);
    } catch (Exception e) {
      this.logger.error("Error en getRendimiento: ", e);
      return new OperationResult<>(new BaseResponse(3, "Error al obtener el rendimiento"), null);
    }
  }

  public OperationResult<List<SelectionUserDTO>> getUsuarios(String filtro, BaseRequest baseRequest) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_BT_SELECCION_USUARIOS");
    try {
      MapSqlParameterSource params = new MapSqlParameterSource()
          .addValue("FILTRO", filtro)
          .addValue("ID_USUARIO", baseRequest.getIdUsuario())
          .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
          .addValue("ID_ROL", baseRequest.getIdRol())
          .addValue("USUARIO", baseRequest.getUsername())
          .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());
      Map<String, Object> result = call.execute(params);

      BaseResponse status = readStatus(result);
      if (status.getIdTipoMensaje() != 2) {
        return new OperationResult<>(status, null);
      }

      List<SelectionUserDTO> list = new ArrayList<>();
      List<Map<String, Object>> rs2 = (List<Map<String, Object>>) result.get("#result-set-2");
      if (rs2 != null) {
        for (Map<String, Object> row : rs2) {
          Object id = row.get("ID_USUARIO");
          list.add(new SelectionUserDTO(id == null ? null : toInt(id),
              (String) row.get("USUARIO"), (String) row.get("NOMBRE"), (String) row.get("EMAIL")));
        }
      }
      return new OperationResult<>(status, list);
    } catch (Exception e) {
      this.logger.error("Error en getUsuarios: ", e);
      return new OperationResult<>(new BaseResponse(3, "Error al obtener los usuarios"), null);
    }
  }

  /** Parámetros comunes de filtro + auditoría. {@code withCliente} agrega ID_CLIENTE. */
  private MapSqlParameterSource baseParams(SelectionFilterRequest request, BaseRequest baseRequest, boolean withCliente) {
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("FCH_INI", blankToNull(request.getFechaIni()))
        .addValue("FCH_FIN", blankToNull(request.getFechaFin()))
        .addValue("ID_USUARIO", baseRequest.getIdUsuario())
        .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
        .addValue("ID_ROL", baseRequest.getIdRol())
        .addValue("USUARIO", baseRequest.getUsername())
        .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());
    if (withCliente) {
      params.addValue("ID_CLIENTE", request.getIdCliente());
    }
    return params;
  }

  private BaseResponse readStatus(Map<String, Object> result) {
    List<Map<String, Object>> rs1 = (List<Map<String, Object>>) result.get("#result-set-1");
    if (rs1 == null || rs1.isEmpty()) {
      return new BaseResponse(3, "Sin respuesta del servidor");
    }
    Map<String, Object> row = rs1.get(0);
    return new BaseResponse(toInt(row.get("ID_TIPO_MENSAJE")), (String) row.get("MENSAJE"));
  }

  private int toInt(Object value) {
    return value instanceof Number ? ((Number) value).intValue() : 0;
  }

  /** Cadena en blanco -> null (fechas vacías = sin filtro de fecha en el SP). */
  private String blankToNull(String value) {
    return (value == null || value.trim().isEmpty()) ? null : value.trim();
  }
}
