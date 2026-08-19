package org.app.autfmi.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.InicioOutsourcingDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Acceso a datos de la notificación de inicio de labores (Outsourcing). Toda la
 * lógica (hitos D-2/D-0, filtro de outsourcing, resolución de destinatarios) vive
 * en SP_BT_NOTIF_INICIO_LST; aquí solo se ejecuta y se mapea el result-set.
 */
@Repository
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class InicioNotificationRepository {

  private final JdbcTemplate jdbcTemplate;
  private final Logger logger = LoggerFactory.getLogger(InicioNotificationRepository.class);

  /** Contratos de outsourcing pendientes de notificar (D-2 o día de inicio). */
  public List<InicioOutsourcingDTO> getPendientes() {
    List<InicioOutsourcingDTO> list = new ArrayList<>();
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_BT_NOTIF_INICIO_LST");
    try {
      Map<String, Object> result = call.execute(new MapSqlParameterSource());
      List<Map<String, Object>> rs = (List<Map<String, Object>>) result.get("#result-set-1");
      if (rs != null) {
        for (Map<String, Object> row : rs) {
          list.add(new InicioOutsourcingDTO(
              toInt(row.get("ID_CONTRATO")),
              (String) row.get("TIPO_HITO"),
              toInt(row.get("NUEVO_ESTADO")),
              (String) row.get("NOMBRES"),
              (String) row.get("APELLIDOS"),
              (String) row.get("DNI"),
              (String) row.get("CELULAR"),
              (String) row.get("EMAIL"),
              (String) row.get("CARGO"),
              (String) row.get("FCH_INICIO"),
              (String) row.get("MODALIDAD"),
              (String) row.get("CLIENTE"),
              (String) row.get("CORREOS_CLIENTE"),
              (String) row.get("CORREOS_GESTORES")));
        }
      }
    } catch (Exception e) {
      this.logger.error("Error al listar contratos pendientes de notificar inicio: ", e);
    }
    return list;
  }

  /** Marca el contrato con el nuevo estado tras enviarse el correo (avance monótono en el SP). */
  public void marcar(int idContrato, int estado) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_BT_NOTIF_INICIO_MARCAR");
    try {
      call.execute(new MapSqlParameterSource()
          .addValue("ID_CONTRATO", idContrato)
          .addValue("ESTADO", estado));
    } catch (Exception e) {
      this.logger.error("Error al marcar notificación de inicio (contrato {}): ", idContrato, e);
    }
  }

  private int toInt(Object value) {
    return value instanceof Number ? ((Number) value).intValue() : 0;
  }
}
