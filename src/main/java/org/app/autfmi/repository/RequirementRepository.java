package org.app.autfmi.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.sqlserver.jdbc.SQLServerDataTable;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.*;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.RequirementListResponse;
import org.app.autfmi.model.response.RequirementResponse;
import org.app.autfmi.model.response.TalentRequirementDataResponse;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.FileUtils;
import org.app.autfmi.util.MailUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RequirementRepository {
    private final JdbcTemplate jdbcTemplate;
    private final MailUtils mailUtils;

    public BaseResponse listRequirements(BaseRequest baseRequest, Integer nPag, Integer cPag, Integer idCliente, String codigoRQ, Date fechaSolicitud, Integer estado) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_REQUERIMIENTO_LST");

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_CLIENTE", idCliente)
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

    public BaseResponse getRequirementById(
            Integer idRequerimiento,
            Boolean showfiles,
            Boolean showVacantesList,
            Boolean showContactList,
            BaseRequest baseRequest
    ) {
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
                                    (Boolean) rqTalentRow.get("CONFIRMADO")
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
                                        FileUtils.cargarArchivo((String) rqFileRow.get("LINK")),
                                        (String) rqFileRow.get("NOMBRE_ARCHIVO"),
                                        (Integer) rqFileRow.get("ID_TIPO_ARCHIVO")
                                );

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
                                        (Integer) vacante.get("CANTIDAD")
                                );

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
                                        (Integer) contacto.get("ASIGNADO")
                                );

                                lstContactos.add(itemContacto);
                            }
                        }
                    }

                    return new RequirementResponse(
                            idTipoMensaje,
                            mensaje,
                            mapToRequirementDTO(requirementData, lstRqTalents, lstRqFiles, lstRqVacantes, lstContactos)
                    );
                }
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    public BaseResponse saveRequirement(RequirementRequest request, BaseRequest baseRequest) throws SQLServerException {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_REQUERIMIENTO_INS");
        SQLServerDataTable tvpRqFiles = loadTvpRequirementFiles(request.getLstArchivos(), baseRequest.getIdEmpresa());
        SQLServerDataTable tvpRqVacantes = loadTvpRequirementVacantes(request.getLstVacantes());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_CLIENTE", request.getIdCliente())
                .addValue("CLIENTE", request.getCliente())
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
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());

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

    public BaseResponse updateRequirement(RequirementRequest request, BaseRequest baseRequest) throws SQLServerException {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_REQUERIMIENTO_UPD");

        SQLServerDataTable tvpRqVacantes = loadTvpRequirementVacantesUpdate(request.getLstVacantes());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_REQUERIMIENTO", request.getIdRequerimiento())
                .addValue("ID_CLIENTE", request.getIdCliente())
                .addValue("CLIENTE", request.getCliente())
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
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());

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

    public BaseResponse saveRequirementTalents(RequirementTalentRequest request, BaseRequest baseRequest) {
        try {
            BaseResponse baseResponse = new BaseResponse();
            SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("SP_REQUERIMIENTO_TALENTO_INS");

            SQLServerDataTable tvpRqTalents = loadTvpRequirementTalents(request);

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("ID_REQUERIMIENTO", request.getIdRequerimiento())
                    .addValue("LST_TALENTOS", tvpRqTalents)
                    .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                    .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                    .addValue("ID_ROL", baseRequest.getIdRol())
                    .addValue("USUARIO", baseRequest.getUsername())
                    .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());

            Map<String, Object> result = simpleJdbcCall.execute(params);
            List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

            if (resultSet != null && !resultSet.isEmpty()) {
                Map<String, Object> row = resultSet.get(0);
                baseResponse.setIdTipoMensaje((Integer) row.get("ID_TIPO_MENSAJE"));
                baseResponse.setMensaje((String) row.get("MENSAJE"));

                if (baseResponse.getIdTipoMensaje() == 2) {
                    List<Map<String, Object>> gestorSet = (List<Map<String, Object>>) result.get("#result-set-2"); // GESTOR
                    List<Map<String, Object>> postulantsSet = (List<Map<String, Object>>) result.get("#result-set-3"); // TALENTOS CONFIRMADOS
                    List<Map<String, Object>> contactosSet = (List<Map<String, Object>>) result.get("#result-set-4"); // CONTACTOS

                    if (postulantsSet != null && !postulantsSet.isEmpty() && gestorSet != null && !gestorSet.isEmpty()) {
                        Map<String, Object> gestorRqRow = gestorSet.get(0);
                        GestorRqDTO gestor = new GestorRqDTO(
                                (String) gestorRqRow.get("NOMBRES"),
                                (String) gestorRqRow.get("APELLIDOS"),
                                (String) gestorRqRow.get("CORREO"),
                                (String) gestorRqRow.get("CODIGO_RQ"),
                                (String) gestorRqRow.get("CLIENTE"),
                                "Ingreso"
                        );

                        List<PostulantDTO> postulantList = new ArrayList<>();
                        if (!postulantsSet.isEmpty()) {
                            for (Map<String, Object> postulantRow : postulantsSet) {
                                postulantList.add(mapListPostulantDTO(postulantRow));
                            }
                        }

                        List<String> contactosList = new ArrayList<>();
                        if (contactosSet != null && !contactosSet.isEmpty()) {
                            for (Map<String, Object> contactoRow : contactosSet) {
                                contactosList.add(
                                    (String) contactoRow.get("CORREO")
                                );
                            }
                        }

                        //ENVIAR CORREO
                        if (request.getFlagCorreo()){
                            mailUtils.sendRequirementPostulantMail(gestor, "Ingreso de nuevo talento", postulantList, contactosList);
                        }
                    }
                }
            }
            return baseResponse;
        } catch (Exception e) {
            return new BaseResponse(3, e.getMessage());
        }
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

                    return new TalentRequirementDataResponse(idTipoMensaje, mensaje, mapTalentRequirementDataDTO(talentRequirementData));
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
                (String) talentoRQ.get("ESTADO")
        );
    }


    public BaseResponse saveRequirementFile(BaseRequest baseRequest, RequirementFileRequest request) throws SQLServerException {
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
                FileUtils.eliminarArchivo(rutaPre);
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
                        }
                );
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return new RequirementItemDTO(
                (Integer) requirement.get("ID_REQUERIMIENTO"),
                (String) requirement.get("CLIENTE"),
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
                perfiles
        );
    }

    private RequirementDTO mapToRequirementDTO(
            Map<String, Object> requerimiento,
            List<RequirementTalentDTO> lstRqTalents,
            List<RequirementFileDTO> lstRqFiles,
            List<RequirementVacanteDTO> lstRqVacantes,
            List<ClientContactItemDTO> lstContactos
    ) {
        return new RequirementDTO(
                (Integer) requerimiento.get("ID_CLIENTE"),
                (String) requerimiento.get("CLIENTE"),
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
                lstRqVacantes,
                lstRqTalents,
                lstRqFiles,
                lstContactos
        );
    }

    private static SQLServerDataTable loadTvpRequirementFiles(List<FileRequest> lstArchivos, Integer idEmpresa) throws SQLServerException {
        SQLServerDataTable tvpRqFiles = new SQLServerDataTable();
        tvpRqFiles.addColumnMetadata("INDICE", Types.INTEGER);
        tvpRqFiles.addColumnMetadata("LINK", Types.VARCHAR);
        tvpRqFiles.addColumnMetadata("NOMBRE_ARCHIVO", Types.VARCHAR);
        tvpRqFiles.addColumnMetadata("ID_TIPO_ARCHIVO", Types.INTEGER);
        int indice = 1;

        for (FileRequest fileRequest : lstArchivos) {
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

    private static SQLServerDataTable loadTvpRequirementVacantes(List<VacanteRequirement> lstVacantes) throws SQLServerException {
        SQLServerDataTable tvpRqVacantes = new SQLServerDataTable();
        tvpRqVacantes.addColumnMetadata("ID_PERFIL", Types.INTEGER);
        tvpRqVacantes.addColumnMetadata("CANTIDAD", Types.INTEGER);

        for (VacanteRequirement vacanteRequirement : lstVacantes) {
            tvpRqVacantes.addRow(
                    vacanteRequirement.getIdPerfil(),
                    vacanteRequirement.getCantidad()
            );
        }

        return tvpRqVacantes;
    }

    private static SQLServerDataTable loadTvpRequirementVacantesUpdate(List<VacanteRequirement> lstVacantes) throws SQLServerException {
        SQLServerDataTable tvpRqVacantes = new SQLServerDataTable();
        tvpRqVacantes.addColumnMetadata("ID_REQUERIMIENTO_VACANTE", Types.INTEGER);
        tvpRqVacantes.addColumnMetadata("ID_PERFIL", Types.INTEGER);
        tvpRqVacantes.addColumnMetadata("CANTIDAD", Types.INTEGER);
        tvpRqVacantes.addColumnMetadata("ID_ESTADO", Types.INTEGER);

        for (VacanteRequirement vacanteRequirement : lstVacantes) {
            tvpRqVacantes.addRow(
                    vacanteRequirement.getIdRequerimientoVacante(),
                    vacanteRequirement.getIdPerfil(),
                    vacanteRequirement.getCantidad(),
                    vacanteRequirement.getIdEstado()
            );
        }

        return tvpRqVacantes;
    }


    @Async
    protected void guardarArchivos(List<FileRequest> lstFiles, Integer idNewRq, Integer idEmpresa) {
        for (FileRequest fileItem : lstFiles) {
            String rutaRq = Constante.RUTA_REPOSITORIO + idEmpresa + Constante.RUTA_RQ_ARCHIVOS.replace("[ID_REQUERIMIENTO]", idNewRq.toString()) + fileItem.getNombreArchivo() + "." + fileItem.getExtensionArchivo();
            FileUtils.guardarArchivo(fileItem.getString64(), rutaRq);
        }
    }


    private static SQLServerDataTable loadTvpRequirementTalents(RequirementTalentRequest request) throws SQLServerException {
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

        int indice = 1;

        for (RequirementTalentRequestDTO talentRequest : request.getLstTalentos()) {
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
                    talentRequest.isConfirmado()? 1 : 0
            );

            indice++;
        }
        return tvpRqTalents;
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
                (BigDecimal) postulanteRow.get("REMUNERACION"),
                (String) postulanteRow.get("MODALIDAD"),
                (String) postulanteRow.get("TIENE_EQUIPO")
        );
    }


    @Async
    public void updateRequirementAlertJob() {
        System.out.println("Ejecutando SP de alertas JOB");
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_REQUERIMIENTO_ALERTA_JOB");
        simpleJdbcCall.execute();
    }

}
