package org.app.autfmi.repository;

import com.microsoft.sqlserver.jdbc.SQLServerDataTable;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.zaxxer.hikari.util.SuspendResumeLock;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.GestorRqDTO;
import org.app.autfmi.model.dto.PostulantDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.MailUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PostulantRepository {
    private final JdbcTemplate jdbcTemplate;
    private final MailUtils mailUtils;

    public BaseResponse registerPostulant(BaseRequest baseRequest, Integer idTalento, List<Integer> lstRequerimientos) {
        BaseResponse baseResponse = new BaseResponse();
        try {
            SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("SP_POSTULANTE_REQUERIMIENTO_INS");

            SQLServerDataTable tvpRqIds = loadTvpRequirementIds(lstRequerimientos);

            SqlParameterSource params = new MapSqlParameterSource()
                    .addValue("ID_TALENTO", idTalento)
                    .addValue("LST_RQ_IDS", tvpRqIds)
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
                    List<Map<String, Object>> resultSet2 = (List<Map<String, Object>>) result.get("#result-set-2");
                    List<Map<String, Object>> resultSet3 = (List<Map<String, Object>>) result.get("#result-set-3");
                    if (resultSet2 != null && !resultSet2.isEmpty() && resultSet3 != null && !resultSet3.isEmpty()) {
                        List<GestorRqDTO> lstGestores = new ArrayList<>();
                        for (Map<String, Object> gestorRqRow : resultSet3) {
                            GestorRqDTO gestor = new GestorRqDTO(
                                    (String) gestorRqRow.get("NOMBRES"),
                                    (String) gestorRqRow.get("APELLIDOS"),
                                    (String) gestorRqRow.get("CORREO"),
                                    (String) gestorRqRow.get("CLIENTE"),
                                    "Ingreso",
                                    (String) gestorRqRow.get("TIENE_EQUIPO")
                            );
                            lstGestores.add(gestor);

                            System.out.println("DATOS DE RESULSET 2");
                            System.out.println(resultSet2);
                            System.out.println(Constante.TXT_SEPARADOR);

                            System.out.println("DATOS DE RESULSET 3");
                            System.out.println(resultSet3);
                            System.out.println(Constante.TXT_SEPARADOR);

                            System.out.println("REEMPLAZANDO DATOS EN HTML BODY");
                            String mensajeCorreo = replaceDataToHtmlBody(Constante.CUERPO_CORREO, gestor, mapPostulantDTO(resultSet2));
                            //Envio de correo asincrono
                            System.out.println("CORREO:::");
                            System.out.println(mensajeCorreo);
                            System.out.println(Constante.TXT_SEPARADOR);
                            mailUtils.sendRequirementPostulantMail(lstGestores, "Ingreso de nuevo talento", mensajeCorreo);
                        }
                    }
                }
            }
            return baseResponse;
        } catch (Exception e) {
            System.err.println("Error en Repository RegisterPostulant");
            System.err.println(e.getMessage());
            return new BaseResponse(3, e.getMessage());
        }
    }

    private static PostulantDTO mapPostulantDTO(List<Map<String, Object>> resultSet) {
        Map<String, Object> postulanteRow = resultSet.get(0);
        return new PostulantDTO(
                (String) postulanteRow.get("NOMBRES"),
                (String) postulanteRow.get("APELLIDO_PATERNO"),
                (String) postulanteRow.get("APELLIDO_MATERNO"),
                (String) postulanteRow.get("CELULAR"),
                (String) postulanteRow.get("EMAIL"),
                (String) postulanteRow.get("DNI"),
                (String) postulanteRow.get("TIEMPO_CONTRATO"),
                (String) postulanteRow.get("FCH_INICIO_LABORES"),
                (String) postulanteRow.get("CARGO"),
                (Double) postulanteRow.get("REMUNERACION"),
                (String) postulanteRow.get("MODALIDAD")
        );
    }

    private static SQLServerDataTable loadTvpRequirementIds(List<Integer> lstRequerimientos) throws SQLServerException {
        SQLServerDataTable tvpRqFiles = new SQLServerDataTable();

        try {
            tvpRqFiles.addColumnMetadata("INDICE", Types.INTEGER);
            tvpRqFiles.addColumnMetadata("ID_REQUERIMIENTO", Types.INTEGER);
            int indice = 1;

            for (Integer idRequerimiento : lstRequerimientos) {
                tvpRqFiles.addRow(
                        indice,
                        idRequerimiento
                );

                indice++;
            }
        } catch (Exception e) {
            System.err.println("Error en TVP RequirementIds");
            System.err.println(e.getMessage());
        }

        return tvpRqFiles;
    }

    private String replaceDataToHtmlBody(String cuerpoCorreo, GestorRqDTO gestor, PostulantDTO postulante) {
        return cuerpoCorreo.replace("[GESTOR]", gestor.getNombres())
                .replace("[CLIENTE]", gestor.getCliente())
                .replace("[TIPO_FORMULARIO]", gestor.getTipoFormulario())
                .replace("[SI_NO_EQUIPO]", gestor.getTieneEquipo())
                .replace("[NOMBRES]", postulante.getNombres())
                .replace("[APELLIDO_PATERNO]", postulante.getApellidoPaterno())
                .replace("[APELLIDO_MATERNO]", postulante.getApellidoMaterno())
                .replace("[DOC_IDENTIDAD]", postulante.getDni())
                .replace("[CELULAR]", postulante.getCelular())
                .replace("[CORREO]", postulante.getEmail())
                .replace("[FCH_INI_LABORES]", postulante.getFechaInicioLabores())
                .replace("[TIEMPO_CONTRATO]", postulante.getTiempoContrato())
                .replace("[CARGO]", postulante.getCargo())
                .replace("[REMUNERACION]", postulante.getRemuneracion().toString())
                .replace("[MODALIDAD]", postulante.getModalidad());
    }
}
