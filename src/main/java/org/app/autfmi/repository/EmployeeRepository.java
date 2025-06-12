package org.app.autfmi.repository;

import com.microsoft.sqlserver.jdbc.SQLServerDataTable;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.EmployeeDTO;
import org.app.autfmi.model.response.SolicitudEquipoResponse;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.SolicitudEquipoRequest;
import org.app.autfmi.model.request.SolicitudSoftwareRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.EmployeeResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class EmployeeRepository {
    private final JdbcTemplate jdbcTemplate;

    public BaseResponse getEmployee(Integer idTalento) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_TALENTO_EMPLEADO_SEL");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_TALENTO", idTalento);

        Map<String, Object> result = simpleJdbcCall.execute(params);

        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");

            if (idTipoMensaje == 2) {
                List<Map<String, Object>> resultSet2 = (List<Map<String, Object>>) result.get("#result-set-2");

                if (resultSet2 != null && !resultSet2.isEmpty()) {
                    Map<String, Object> employeeRaw = resultSet2.get(0);
                    return new EmployeeResponse( idTipoMensaje, mensaje, mapToEmployeeDTO(employeeRaw) );
                }
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    private EmployeeDTO mapToEmployeeDTO(Map<String, Object> employeeRaw) {
        return new EmployeeDTO(
                (String) employeeRaw.get("NOMBRES"),
                (String) employeeRaw.get("APELLIDO_PATERNO"),
                (String) employeeRaw.get("APELLIDO_MATERNO"),
                (Integer) employeeRaw.get("ID_AREA"),
                (Double) employeeRaw.get("REMUNERACION"),
                (Integer) employeeRaw.get("ID_CLIENTE"),
                (String) employeeRaw.get("CARGO")
        );
    }

    public SolicitudEquipoResponse solicitudEquipo(BaseRequest baseRequestequest, SolicitudEquipoRequest solicitudEquipoRequest) throws SQLServerException {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_EQUIPO_SOLICITUD_INS");

        SolicitudEquipoResponse solicitudEquipoResponse = new SolicitudEquipoResponse();
        BaseResponse baseResponse = new BaseResponse();

        SQLServerDataTable tvpProductos = getSqlServerDataTable(solicitudEquipoRequest);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_TALENTO", solicitudEquipoRequest.getIdTalento())
                .addValue("NOMBRE_EMPLEADO", solicitudEquipoRequest.getNombreEmpleado())
                .addValue("APELLIDO_PATERNO_EMPLEADO", solicitudEquipoRequest.getApellidoPaternoEmpleado())
                .addValue("APELLIDO_MATERNO_EMPLEADO", solicitudEquipoRequest.getApellidoMaternoEmpleado())
                .addValue("ID_CLIENTE", solicitudEquipoRequest.getIdCliente())
                .addValue("CLIENTE", solicitudEquipoRequest.getCliente())
                .addValue("ID_AREA", solicitudEquipoRequest.getIdArea())
                .addValue("AREA", solicitudEquipoRequest.getArea())
                .addValue("PUESTO", solicitudEquipoRequest.getPuesto())
                .addValue("FECHA_SOLICITUD", solicitudEquipoRequest.getFechaSolicitud())
                .addValue("FECHA_ENTREGA", solicitudEquipoRequest.getFechaEntrega())
                .addValue("ID_TIPO_EQUIPO", solicitudEquipoRequest.getIdTipoEquipo())
                .addValue("TIPO_EQUIPO", solicitudEquipoRequest.getTipoEquipo())
                .addValue("PROCESADOR", solicitudEquipoRequest.getProcesador())
                .addValue("RAM", solicitudEquipoRequest.getRam())
                .addValue("HD", solicitudEquipoRequest.getHd())
                .addValue("MARCA", solicitudEquipoRequest.getMarca())
                .addValue("ID_ANEXO", solicitudEquipoRequest.getIdAnexo())
                .addValue("ANEXO", solicitudEquipoRequest.getAnexo())
                .addValue("CELULAR", solicitudEquipoRequest.getCelular())
                .addValue("INTERNET_MOVIL", solicitudEquipoRequest.getInternetMovil())
                .addValue("ACCESORIOS", solicitudEquipoRequest.getAccesorios())
                .addValue("LST_SOFTWARE", tvpProductos)
                .addValue("ID_ROL", baseRequestequest.getIdRol())
                .addValue("ID_FUNCIONALIDADES", baseRequestequest.getFuncionalidades())
                .addValue("ID_USUARIO", baseRequestequest.getIdUsuario())
                .addValue("ID_EMPRESA", baseRequestequest.getIdEmpresa())
                .addValue("USERNAME", baseRequestequest.getUsername());

        Map<String, Object> result = simpleJdbcCall.execute(params);

        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");

        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            baseResponse.setIdTipoMensaje((Integer) row.get("ID_TIPO_MENSAJE"));
            baseResponse.setMensaje((String) row.get("MENSAJE"));

            if (baseResponse.getIdTipoMensaje() == 2) {
                solicitudEquipoResponse.setNombres((String) row.get("NOMBRES_FIRMANTE"));
                solicitudEquipoResponse.setApellidos((String) row.get("APELLIDOS_FIRMANTE"));
                solicitudEquipoResponse.setCorreoGestor((String) row.get("EMAIL_FIRMANTE"));
            }
        }

        solicitudEquipoResponse.setBaseResponse(baseResponse);

        return solicitudEquipoResponse;
    }

    private static SQLServerDataTable getSqlServerDataTable(SolicitudEquipoRequest solicitudEquipoRequest) throws SQLServerException {
        SQLServerDataTable tvpProductos = new SQLServerDataTable();
        tvpProductos.addColumnMetadata("ID_ITEM", Types.INTEGER);
        tvpProductos.addColumnMetadata("PRODUCTO", Types.VARCHAR);
        tvpProductos.addColumnMetadata("PROD_VERSION", Types.VARCHAR);

        for (SolicitudSoftwareRequest softwareRequest : solicitudEquipoRequest.getLstSoftware()) {
            tvpProductos.addRow(
                    softwareRequest.getIdItem(),
                    softwareRequest.getProducto(),
                    softwareRequest.getProdVersion()
            );
        }
        return tvpProductos;
    }
}
