package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.*;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.TalentRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.TalentListResponse;
import org.app.autfmi.model.response.TalentRequirementListResponse;
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

    public BaseResponse listTalents(BaseRequest baseRequest, Integer nPag, String busqueda) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_USUARIOS_TALENTOS_LST");

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("N_PAG", nPag)
                .addValue("BUSQUEDA", busqueda);

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");
            Integer totalElementos = (Integer) row.get("TOTAL_ELEMENTOS");
            Integer totalPaginas = (Integer) row.get("TOTAL_PAGINAS");

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> talentsSet = (List<Map<String, Object>>) result.get("#result-set-2");
                List<TalentItemDTO> talentList = new ArrayList<>();
                if (talentsSet != null && !talentsSet.isEmpty()) {
                    for (Map<String, Object> talentRow : talentsSet) {
                        talentList.add(mapToTalentItemDTO(talentRow));
                    }
                }
                return new TalentListResponse(idTipoMensaje, mensaje, talentList, totalElementos, totalPaginas);
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public BaseResponse getTalentById(Integer idTalento, BaseRequest baseRequest) {
        // GET FROM BT_TALENTO
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
        // UPDATE BT_TALENTO, REQ_TALENTO -> CONFIRMADO 0, INGRESO 0
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_USUARIOS_TALENTOS_INS");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("ID_TALENTO", talent.getIdTalento())
                .addValue("NOMBRES", talent.getNombres())
                .addValue("APELLIDO_PATERNO", talent.getApellidoPaterno())
                .addValue("APELLIDO_MATERNO", talent.getApellidoMaterno())
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
                .addValue("TIENE_EQUIPO", talent.isTieneEquipo())
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

    public BaseResponse getTalentsToRequirementList(BaseRequest baseRequest, Integer nPag, String busqueda) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_FMI_TALENTO_RQ_LST");

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("N_PAG", nPag)
                .addValue("BUSQUEDA", busqueda)
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
                List<Map<String, Object>> talentsSet = (List<Map<String, Object>>) result.get("#result-set-2");
                List<TalentRequirementItemDTO> talentList = new ArrayList<>();
                if (talentsSet != null && !talentsSet.isEmpty()) {
                    for (Map<String, Object> talentRow : talentsSet) {
                        talentList.add(mapToTalentRequirementItemDTO(talentRow));
                    }
                }
                return new TalentRequirementListResponse(idTipoMensaje, mensaje, talentList);
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    private TalentDTO mapToTalentDTO(Map<String, Object> talent) {
        return new TalentDTO(
                (String) talent.get("NOMBRES"),
                (String) talent.get("APELLIDO_PATERNO"),
                (String) talent.get("APELLIDO_MATERNO"),
                (String) talent.get("CELULAR"),
                (String) talent.get("EMAIL"),
                (String) talent.get("DNI"),
                (String) talent.get("CARGO"),
                (Double) talent.get("REMUNERACION"),
                (Integer) talent.get("ID_MONEDA")
        );
    }

    private TalentItemDTO mapToTalentItemDTO(Map<String, Object> talent) {
        return new TalentItemDTO(
                (Integer) talent.get("ID_TALENTO"),
                (Integer) talent.get("ID_EQUIPO_SOLICITUD"),
                (String) talent.get("NOMBRES"),
                (String) talent.get("APELLIDOS"),
                (String) talent.get("MODALIDAD")
        );
    }

    private TalentRequirementItemDTO mapToTalentRequirementItemDTO(Map<String, Object> talent) {
        return new TalentRequirementItemDTO(
                (Integer) talent.get("ID_TALENTO"),
                (String) talent.get("NOMBRES"),
                (String) talent.get("APELLIDO_PATERNO"),
                (String) talent.get("APELLIDO_MATERNO")
        );
    }
}
