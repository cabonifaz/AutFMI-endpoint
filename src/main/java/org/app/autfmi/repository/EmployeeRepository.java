package org.app.autfmi.repository;

import com.microsoft.sqlserver.jdbc.SQLServerDataTable;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import lombok.RequiredArgsConstructor;

import org.app.autfmi.model.dto.EmployeeDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.SolicitudEquipoRequest;
import org.app.autfmi.model.request.SolicitudSoftwareRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.EmployeeResponse;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.model.response.TalentEmployeeList;
import org.app.autfmi.model.response.TalentEmployeeList.EmployeeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class EmployeeRepository {

    @NonNull
    private final JdbcTemplate jdbcTemplate;

    private Logger logger = LoggerFactory.getLogger(EmployeeRepository.class);

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
                    return new EmployeeResponse(idTipoMensaje, mensaje, mapToEmployeeDTO(employeeRaw));
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
                (String) employeeRaw.get("CARGO"));
    }

    public OperationResult<Integer> insertSolicitudEquipo(BaseRequest baseRequestequest,
            SolicitudEquipoRequest solicitudEquipoRequest) throws SQLServerException {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_EQUIPO_SOLICITUD_INS");

        SQLServerDataTable tvpProductos = getSqlServerDataTable(solicitudEquipoRequest);
        BaseResponse baseResponse = new BaseResponse();

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

        if (resultSet == null || resultSet.isEmpty()) {
            baseResponse = new BaseResponse(3, "La base de datos no retornó información.");
            return new OperationResult<>(baseResponse, null);
        }

        Map<String, Object> row = resultSet.get(0);
        Integer messageId = (Integer) row.get("ID_TIPO_MENSAJE");
        String message = (String) row.get("MENSAJE");
        Integer operationId = (Integer) row.get("ID_OPERACION");
        baseResponse = new BaseResponse(messageId, message);

        return new OperationResult<>(baseResponse, operationId);
    }

    private static SQLServerDataTable getSqlServerDataTable(SolicitudEquipoRequest solicitudEquipoRequest)
            throws SQLServerException {
        SQLServerDataTable tvpProductos = new SQLServerDataTable();
        tvpProductos.addColumnMetadata("ID_TALENTO", Types.INTEGER);
        tvpProductos.addColumnMetadata("ID_ITEM", Types.INTEGER);
        tvpProductos.addColumnMetadata("PRODUCTO", Types.VARCHAR);
        tvpProductos.addColumnMetadata("PROD_VERSION", Types.VARCHAR);

        for (SolicitudSoftwareRequest softwareRequest : solicitudEquipoRequest.getLstSoftware()) {
            tvpProductos.addRow(
                    softwareRequest.getIdItem(),
                    softwareRequest.getIdItem(),
                    softwareRequest.getProducto(),
                    softwareRequest.getProdVersion());
        }
        return tvpProductos;
    }

    public TalentEmployeeList findAllEmployees(BaseRequest baseRequest, Integer page, String searchTerm) {
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_TALENTO_CTR_LST");

        this.logger.info("Fetching Talents with contract");

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                .addValue("N_PAG", page)
                .addValue("BUSQUEDA", searchTerm);

        Map<String, Object> result = simpleJdbcCall.execute(params);
        List<Map<String, Object>> baseRs = (List<Map<String, Object>>) result.get("#result-set-1");

        if (baseRs == null || baseRs.isEmpty())
            return new TalentEmployeeList(3, "No hay respuesta de la base de datos");

        Map<String, Object> row = baseRs.get(0);
        Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
        String mensaje = (String) row.get("MENSAJE");
        Integer totalElementos = (Integer) row.get("TOTAL_ELEMENTOS");
        Integer totalPaginas = (Integer) row.get("TOTAL_PAGINAS");

        if (idTipoMensaje != 2)
            return new TalentEmployeeList(idTipoMensaje, mensaje);

        List<Map<String, Object>> employeeSet = (List<Map<String, Object>>) result.getOrDefault("#result-set-2",
                Collections.emptyList());

        List<EmployeeItem> employees = employeeSet
                .stream()
                .map(this::mapToEmployeeItem)
                .collect(Collectors.toList());

        return new TalentEmployeeList(idTipoMensaje, mensaje)
                .withMetadata(totalPaginas, totalElementos)
                .withTalents(employees);
    }

    private EmployeeItem mapToEmployeeItem(Map<String, Object> row) {

        Integer talentId = (Integer) row.get("ID_TALENTO");
        String names = (String) row.get("NOMBRES");
        String lastname = (String) row.get("APELLIDO_PATERNO");
        String surname = (String) row.get("APELLIDO_MATERNO");
        String fullname = lastname + " " + surname;

        return new EmployeeItem(talentId, names, fullname);
    }
}
