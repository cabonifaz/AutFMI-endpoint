package org.app.autfmi.repository;

import org.app.autfmi.model.dto.TalentDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.TalentRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.TalentListResponse;
import org.app.autfmi.model.response.TalentResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class TalentRepository {
    private final JdbcTemplate jdbcTemplate;

    public TalentRepository(@Qualifier("devJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BaseResponse listTalents(BaseRequest baseRequest) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_USUARIOS_TALENTOS_LST");

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("TIPO_ROL", baseRequest.getTipoRol())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa());

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                return getTalentsList(result, idTipoMensaje, mensaje);
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public BaseResponse getTalentById(TalentRequest talentRequest, BaseRequest baseRequest) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_USUARIOS_TALENTOS_SEL");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_USUARIO_TALENTO", talentRequest.getIdTalento())
                .addValue("ID_EMPRESA", talentRequest.getIdEmpresa())
                .addValue("TIPO_ROL", baseRequest.getTipoRol())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("ID_USUARIO", baseRequest.getIdUsuario());

        Map<String, Object> result = simpleJdbcCall.execute(params);

        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                return getTalent(result, idTipoMensaje, mensaje);
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    private TalentDTO MapToTalentDTO(Map<String, Object> talent) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Formatear fecha
        String formattedDate = null;
        Object fchInicioLabores = talent.get("FCH_INICIO_LABORES");
        if (fchInicioLabores instanceof Timestamp) {
            LocalDate localDate = ((Timestamp) fchInicioLabores).toLocalDateTime().toLocalDate();
            formattedDate = localDate.format(dateFormatter);
        }

        // Formatear remuneración
        Double remuneracion = null;
        Object remuneracionObj = talent.get("REMUNERACION");
        if (remuneracionObj instanceof BigDecimal) {
            remuneracion = ((BigDecimal) remuneracionObj).doubleValue();
        }

        return new TalentDTO(
                (Integer) talent.get("ID_USUARIO_TALENTO"),
                (String) talent.get("NOMBRES"),
                (String) talent.get("APELLIDOS"),
                (String) talent.get("TELEFONO"),
                (String) talent.get("EMAIL"),
                (String) talent.get("DNI"),
                (Integer) talent.get("TIEMPO_CONTRATO"),
                (String) talent.get("STR_TIEMPO_CONTRATO"),
                formattedDate,
                (String) talent.get("CARGO"),
                remuneracion,
                (String) talent.get("MONEDA"),
                (String) talent.get("MODALIDAD"),
                (String) talent.get("UBICACION")
        );
    }

    private TalentResponse getTalent(Map<String, Object> rawData , Integer idTipoMensaje, String mensaje) {
        List<Map<String, Object>> resultSet2 = (List<Map<String, Object>>) rawData.get("#result-set-2");

        if (resultSet2 != null && !resultSet2.isEmpty()) {
            Map<String, Object> talentRaw = resultSet2.get(0);
            return new TalentResponse(idTipoMensaje, mensaje, MapToTalentDTO(talentRaw));
        }
        return null;
    }

    private TalentListResponse getTalentsList(Map<String, Object> rawData, Integer idTipoMensaje, String mensaje) {
        List<Map<String, Object>> talentsSet = (List<Map<String, Object>>) rawData.get("#result-set-2");
        if (talentsSet != null && !talentsSet.isEmpty()) {
            List<TalentDTO> talentList = new ArrayList<>();

            for (Map<String, Object> row : talentsSet) {
                talentList.add(MapToTalentDTO(row));
            }
            return new TalentListResponse(idTipoMensaje, mensaje, talentList);
        }
        return null;
    }
}
