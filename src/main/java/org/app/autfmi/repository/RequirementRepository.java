package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.*;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.RequirementRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.RequirementListResponse;
import org.app.autfmi.model.response.RequirementResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RequirementRepository {
    private final JdbcTemplate jdbcTemplate;

    public BaseResponse listRequirements(BaseRequest baseRequest, Integer nPag, Integer cPag, String cliente, String codigoRQ, Date fechaSolicitud, Integer estado) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_REQUERIMIENTO_LST");

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("CLIENTE", cliente)
                .addValue("CODIGO_RQ", codigoRQ)
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

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> requirementSet = (List<Map<String, Object>>) result.get("#result-set-2");
                if (requirementSet != null && !requirementSet.isEmpty()) {
                    List<RequirementItemDTO> requirementList = new ArrayList<>();

                    for (Map<String, Object> requirementRow : requirementSet) {
                        requirementList.add(mapToRequirementItemDTO(requirementRow));
                    }
                    return new RequirementListResponse(idTipoMensaje, mensaje, requirementList);
                }
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public BaseResponse getRequirementById(Integer idRequerimiento, BaseRequest baseRequest) {
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
                                    (String) rqTalentRow.get("ESTADO")
                            );

                            lstRqTalents.add(itemRqTalento);
                        }
                    }

                    List<Map<String, Object>> resultSet4 = (List<Map<String, Object>>) result.get("#result-set-4");
                    List<RequirementFileDTO> lstRqFiles = new ArrayList<>();

                    if (resultSet4 != null && !resultSet4.isEmpty()) {
                        for (Map<String, Object> rqFileRow : resultSet4) {
                            RequirementFileDTO itemRqArchivo = new RequirementFileDTO(
                                    (Integer) rqFileRow.get("ID_REQUERIMIENTO_ARCHIVO"),
                                    (String) rqFileRow.get("LINK"),
                                    (String) rqFileRow.get("NOMBRE_ARCHIVO"),
                                    (Integer) rqFileRow.get("ID_TIPO_ARCHIVO")
                            );

                            lstRqFiles.add(itemRqArchivo);
                        }
                    }

                    return new RequirementResponse(idTipoMensaje, mensaje, mapToRequirementDTO(requirementData, lstRqTalents, lstRqFiles));
                }
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public BaseResponse saveRequirement(RequirementRequest request, BaseRequest baseRequest) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_REQUERIMIENTO_INS");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("CLIENTE", request.getCliente())
                .addValue("CODIGO_RQ", request.getCodigoRQ())
                .addValue("FECHA_SOLICITUD", request.getFechaSolicitud())
                .addValue("DESCRIPCION", request.getDescripcion())
                .addValue("ESTADO", request.getEstado())
                .addValue("VACANTES", request.getVacantes())
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
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


    private RequirementItemDTO mapToRequirementItemDTO(Map<String, Object> requirement) {
        return new RequirementItemDTO(
                (Integer) requirement.get("ID_REQUERIMIENTO"),
                (String) requirement.get("CLIENTE"),
                (String) requirement.get("CODIGO_RQ"),
                (Date) requirement.get("FECHA_SOLICITUD"),
                (Integer) requirement.get("ESTADO"),
                (Integer) requirement.get("VACANTES")
        );
    }

    private RequirementDTO mapToRequirementDTO(Map<String, Object> requerimiento, List<RequirementTalentDTO> lstRqTalents, List<RequirementFileDTO> lstRqFiles) {
        return new RequirementDTO(
                (String) requerimiento.get("CLIENTE"),
                (String) requerimiento.get("CODIGO_RQ"),
                (Date) requerimiento.get("FECHA_SOLICITUD"),
                (String) requerimiento.get("DESCRIPCION"),
                (Integer) requerimiento.get("ESTADO"),
                (Integer) requerimiento.get("VACANTES"),
                lstRqTalents,
                lstRqFiles
        );
    }
}
