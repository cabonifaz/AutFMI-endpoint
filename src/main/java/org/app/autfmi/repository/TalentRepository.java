package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.*;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.TalentRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.TalentListResponse;
import org.app.autfmi.model.response.TalentResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class TalentRepository {
    private final JdbcTemplate jdbcTemplate;

    public BaseResponse listTalents(BaseRequest baseRequest, Integer nPag) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_USUARIOS_TALENTOS_LST");

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("N_PAG", nPag);

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> talentsSet = (List<Map<String, Object>>) result.get("#result-set-2");
                if (talentsSet != null && !talentsSet.isEmpty()) {
                    List<TalentItemDTO> talentList = new ArrayList<>();

                    for (Map<String, Object> talentRow : talentsSet) {
                        talentList.add(mapToTalentItemDTO(talentRow));
                    }
                    return new TalentListResponse(idTipoMensaje, mensaje, talentList);
                }
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public BaseResponse getTalentById(Integer idTalento, BaseRequest baseRequest) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_USUARIOS_TALENTOS_SEL");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_TALENTO", idTalento)
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("ID_USUARIO", baseRequest.getIdUsuario());

        Map<String, Object> result = simpleJdbcCall.execute(params);

        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> resultSet2 = (List<Map<String, Object>>) result.get("#result-set-2");

                if (resultSet2 != null && !resultSet2.isEmpty()) {
                    Map<String, Object> talentRaw = resultSet2.get(0);
                    return new TalentResponse( idTipoMensaje, mensaje, mapToTalentDTO(talentRaw) );
                }
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public BaseResponse saveTalent(TalentRequest talent, BaseRequest baseRequest) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_USUARIOS_TALENTOS_INS");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("ID_TALENTO", talent.getIdTalento())
                .addValue("NOMBRES", talent.getNombres())
                .addValue("APELLIDOS", talent.getApellidos())
                .addValue("TELEFONO", talent.getTelefono())
                .addValue("DNI", talent.getDni())
                .addValue("EMAIL", talent.getEmail())
                .addValue("TIEMPO_CONTRATO", talent.getTiempoContrato())
                .addValue("ID_TIPO_TIEMPO_CONTRATO", talent.getIdTiempoContrato())
                .addValue("FCH_INICIO_LABORES", talent.getFechaInicioLabores())
                .addValue("CARGO", talent.getCargo())
                .addValue("REMUNERACION", talent.getRemuneracion())
                .addValue("ID_TIPO_MONEDA", talent.getIdMoneda())
                .addValue("ID_MODALIDAD", talent.getIdModalidad())
                .addValue("UBICACION", talent.getUbicacion())
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("ID_USUARIO", baseRequest.getIdUsuario());

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    private TalentDTO mapToTalentDTO(Map<String, Object> talent) {
        return new TalentDTO(
                (String) talent.get("NOMBRES"),
                (String) talent.get("APELLIDOS"),
                (String) talent.get("TELEFONO"),
                (String) talent.get("EMAIL"),
                (String) talent.get("DNI"),
                (Integer) talent.get("TIEMPO_CONTRATO"),
                (Integer) talent.get("ID_TIPO_TIEMPO_CONTRATO"),
                (String) talent.get("FCH_INICIO_LABORES"),
                (String) talent.get("CARGO"),
                (Double) talent.get("REMUNERACION"),
                (Integer) talent.get("ID_TIPO_MONEDA"),
                (Integer) talent.get("ID_MODALIDAD"),
                (String) talent.get("UBICACION")
        );
    }

    private TalentItemDTO mapToTalentItemDTO(Map<String, Object> talent) {
        Boolean esTrabajador = ((Integer) talent.get("ES_TRABAJADOR")) == 1;

        return new TalentItemDTO(
                (Integer) talent.get("ID_USUARIO_TALENTO"),
                (Integer) talent.get("ID_TALENTO"),
                esTrabajador,
                (String) talent.get("NOMBRES"),
                (String) talent.get("APELLIDOS"),
                (String) talent.get("MODALIDAD")
        );
    }
}
