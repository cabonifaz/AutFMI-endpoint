package org.app.autfmi.repository;

import com.microsoft.sqlserver.jdbc.SQLServerDataTable;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.*;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.RequirementListResponse;
import org.app.autfmi.model.response.RequirementResponse;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.FileUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.sql.Types;
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
                List<RequirementItemDTO> requirementList = new ArrayList<>();
                if (requirementSet != null && !requirementSet.isEmpty()) {
                    for (Map<String, Object> requirementRow : requirementSet) {
                        requirementList.add(mapToRequirementItemDTO(requirementRow));
                    }
                }
                return new RequirementListResponse(idTipoMensaje, mensaje, requirementList);
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public BaseResponse getRequirementById(Integer idRequerimiento, Boolean showfiles, BaseRequest baseRequest) {
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

                    List<RequirementFileDTO> lstRqFiles = new ArrayList<>();
                    if (showfiles) {
                        List<Map<String, Object>> resultSet4 = (List<Map<String, Object>>) result.get("#result-set-4");

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
                    }


                    return new RequirementResponse(idTipoMensaje, mensaje, mapToRequirementDTO(requirementData, lstRqTalents, lstRqFiles));
                }
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public BaseResponse saveRequirement(RequirementRequest request, BaseRequest baseRequest) throws SQLServerException {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_REQUERIMIENTO_INS");
        SQLServerDataTable tvpRqFiles = loadTvpRequirementFiles(request, baseRequest.getIdEmpresa());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("CLIENTE", request.getCliente())
                .addValue("CODIGO_RQ", request.getCodigoRQ())
                .addValue("FECHA_SOLICITUD", request.getFechaSolicitud())
                .addValue("DESCRIPCION", request.getDescripcion())
                .addValue("ESTADO", request.getEstado())
                .addValue("VACANTES", request.getVacantes())
                .addValue("LST_ARCHIVOS", tvpRqFiles)
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
            if (idTipoMensaje == 2) {
                Integer idNuevoRQ = (Integer) row.get("ID_NEW_RQ");
                guardarArchivos(request.getLstArchivos(), idNuevoRQ, baseRequest.getIdEmpresa());
            }

            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public BaseResponse saveRequirementTalents(RequirementTalentRequest request, BaseRequest baseRequest) throws SQLServerException {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_REQUERIMIENTO_TALENTO_INS");
        SQLServerDataTable tvpRqTalents = loadTvpRequirementTalents(request);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("LST_TALENTOS", tvpRqTalents)
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
                (String) requirement.get("FECHA_SOLICITUD"),
                (String) requirement.get("ESTADO"),
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

    private static SQLServerDataTable loadTvpRequirementFiles(RequirementRequest request, Integer idEmpresa) throws SQLServerException {
        SQLServerDataTable tvpRqFiles = new SQLServerDataTable();
        tvpRqFiles.addColumnMetadata("INDICE", Types.INTEGER);
        tvpRqFiles.addColumnMetadata("LINK", Types.VARCHAR);
        tvpRqFiles.addColumnMetadata("NOMBRE_ARCHIVO", Types.VARCHAR);
        tvpRqFiles.addColumnMetadata("ID_TIPO_ARCHIVO", Types.INTEGER);
        int indice = 1;

        for (FileRequest fileRequest : request.getLstArchivos()) {
            String rutaArchivo = Constante.RUTA_REPOSITORIO + idEmpresa + Constante.RUTA_RQ_ARCHIVOS + fileRequest.getNombreArchivo() + "." + fileRequest.getExtensionArchivo();

            tvpRqFiles.addRow(
                    indice,
                    rutaArchivo,
                    fileRequest.getNombreArchivo(),
                    fileRequest.getIdTipoArchivo()
            );

            indice++;
        }
        return tvpRqFiles;
    }


    @Async
    protected void guardarArchivos(List<FileRequest> lstFiles, Integer idNewRq, Integer idEmpresa) {
        for (FileRequest fileItem : lstFiles) {
            String rutaRq = Constante.RUTA_REPOSITORIO + idEmpresa + Constante.RUTA_RQ_ARCHIVOS.replace("[ID_REQUERIMIENTO]", idNewRq.toString()) + fileItem.getNombreArchivo() + "." + fileItem.getExtensionArchivo();
            FileUtils.guardarArchivo(fileItem.getString64(),rutaRq);
        }
    }


    private static SQLServerDataTable loadTvpRequirementTalents(RequirementTalentRequest request) throws SQLServerException {
        SQLServerDataTable tvpRqTalents = new SQLServerDataTable();
        tvpRqTalents.addColumnMetadata("INDICE", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("ID_REQUERIMIENTO", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("ID_TALENTO", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("NOMBRES_TALENTO", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("APELLIDOS_TALENTO", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("DNI", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("CELULAR", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("EMAIL", Types.VARCHAR);
        tvpRqTalents.addColumnMetadata("ID_SITUACION", Types.INTEGER);
        tvpRqTalents.addColumnMetadata("ID_ESTADO", Types.INTEGER);

        int indice = 1;

        for (RequirementTalentRequestDTO talentRequest : request.getLstTalentos()) {
            tvpRqTalents.addRow(
                    indice,
                    request.getIdRequerimiento(),
                    talentRequest.getIdTalento(),
                    talentRequest.getNombres(),
                    talentRequest.getApellidos(),
                    talentRequest.getDni(),
                    talentRequest.getCelular(),
                    talentRequest.getEmail(),
                    talentRequest.getIdSituacion(),
                    talentRequest.getIdEstado()
            );

            indice++;
        }
        return tvpRqTalents;
    }

}
