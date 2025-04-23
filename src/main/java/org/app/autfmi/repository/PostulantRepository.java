package org.app.autfmi.repository;

import com.microsoft.sqlserver.jdbc.SQLServerDataTable;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.RequirementPostulantRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.util.MailUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PostulantRepository {
    private final JdbcTemplate jdbcTemplate;
    private final MailUtils mailUtils;

    public BaseResponse registerPostulant(BaseRequest baseRequest, Integer idTalento, List<RequirementPostulantRequest> lstRequerimientos) {
        BaseResponse baseResponse = new BaseResponse();
        try {
            SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("SP_POSTULANTE_REQUERIMIENTO_INS");

            SQLServerDataTable tvpRqPostulante = loadTvpRequirementPostulant(lstRequerimientos);

            SqlParameterSource params = new MapSqlParameterSource()
                    .addValue("ID_TALENTO", idTalento)
                    .addValue("LST_RQ_POSTULANTE", tvpRqPostulante)
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
            }

            return baseResponse;
        } catch (Exception e) {
            System.err.println("Error en Repository RegisterPostulant");
            System.err.println(e.getMessage());
            return new BaseResponse(3, e.getMessage());
        }
    }
    private static SQLServerDataTable loadTvpRequirementPostulant(List<RequirementPostulantRequest> lstRequerimientos) throws SQLServerException {
        SQLServerDataTable tvpRqPostulant = new SQLServerDataTable();

        try {
            tvpRqPostulant.addColumnMetadata("INDICE", Types.INTEGER);
            tvpRqPostulant.addColumnMetadata("ID_REQUERIMIENTO", Types.INTEGER);
            tvpRqPostulant.addColumnMetadata("ID_PERFIL", Types.INTEGER);
            int indice = 1;

            for (RequirementPostulantRequest itemRq : lstRequerimientos) {
                tvpRqPostulant.addRow(
                        indice,
                        itemRq.getIdRQ(),
                        itemRq.getIdPerfil()
                );

                indice++;
            }
        } catch (Exception e) {
            System.err.println("Error en TVP RequirementIds");
            System.err.println(e.getMessage());
        }

        return tvpRqPostulant;
    }


}
