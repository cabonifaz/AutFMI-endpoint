package org.app.autfmi.repository;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.ClientContactItemDTO;
import org.app.autfmi.model.dto.ClientItemDTO;
import org.app.autfmi.model.dto.RequirementItemDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.ContactRegisterRequest;
import org.app.autfmi.model.request.ContactUpdateRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.ClientContactListResponse;
import org.app.autfmi.model.response.ClientListResponse;
import org.app.autfmi.model.response.RequirementListResponse;
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
public class ClientRepository {
    private final JdbcTemplate jdbcTemplate;

    public BaseResponse listClients(BaseRequest baseRequest){
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_CLIENTE_LST");

        SqlParameterSource params = new MapSqlParameterSource()
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
                List<Map<String, Object>> clientSet = (List<Map<String, Object>>) result.get("#result-set-2");
                List<ClientItemDTO> clientList = new ArrayList<>();
                if (clientSet != null && !clientSet.isEmpty()) {
                    for (Map<String, Object> clientRow : clientSet) {
                        clientList.add(mapClientItemDTO(clientRow));
                    }
                }
                return new ClientListResponse(idTipoMensaje, mensaje, clientList);
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return null;
    }

    private ClientItemDTO mapClientItemDTO(Map<String, Object> client) {
        return new ClientItemDTO(
                (Integer) client.get("ID_CLIENTE"),
                (String) client.get("RAZON_SOCIAL"),
                (Integer) client.get("TOTAL")
        );
    }

    public BaseResponse listClientContacts(BaseRequest baseRequest, Integer idCliente){
        SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate).withProcedureName("SP_CLIENTE_CONTACTO_LST");

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("ID_CLIENTE", idCliente)
                .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                .addValue("ID_ROL", baseRequest.getIdRol())
                .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());

        Map<String, Object> result = simpleJdbcCall.execute(params);

        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");
        if (resultSet != null && !resultSet.isEmpty()) {
            Map<String, Object> row = resultSet.get(0);
            Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
            String mensaje = (String) row.get("MENSAJE");
            if (idTipoMensaje == 2) {
                List<Map<String, Object>> clientSet = (List<Map<String, Object>>) result.get("#result-set-2");
                List<ClientContactItemDTO> clientContacts = new ArrayList<>();

                if (clientSet != null && !clientSet.isEmpty()) {
                    for (Map<String, Object> clientContactRow : clientSet) {
                        ClientContactItemDTO clientContact = new ClientContactItemDTO(
                                (Integer) clientContactRow.get("ID_CLIENTE_CONTACTO"),
                                (String) clientContactRow.get("NOMBRES"),
                                (String) clientContactRow.get("APELLIDO_PATERNO"),
                                (String) clientContactRow.get("APELLIDO_MATERNO"),
                                (String) clientContactRow.get("CARGO"),
                                (String) clientContactRow.get("TELEFONO"),
                                (String) clientContactRow.get("TELEFONO_2"),
                                (String) clientContactRow.get("CORREO"),
                                (String) clientContactRow.get("CORREO_2"),
                                (Integer) clientContactRow.get("ID_ESTADO_CONTACTO")
                        );

                        clientContacts.add(clientContact);
                    }
                }
                return new ClientContactListResponse(idTipoMensaje, mensaje, clientContacts);
            }
            return new BaseResponse(idTipoMensaje, mensaje);
        }
        return new BaseResponse(3, "Error al conectarse a base datos");
    }

    public BaseResponse saveContact(BaseRequest baseRequest, ContactRegisterRequest contacto) {
        try {
            SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("SP_CLIENTE_CONTACTO_INS");

            SqlParameterSource params = new MapSqlParameterSource()
                    .addValue("ID_CLIENTE", contacto.getIdCliente())
                    .addValue("NOMBRES",contacto.getNombres())
                    .addValue("APELLIDO_PATERNO",contacto.getApellidoPaterno())
                    .addValue("APELLIDO_MATERNO",contacto.getApellidoMaterno())
                    .addValue("CARGO",contacto.getCargo())
                    .addValue("TELEFONO",contacto.getTelefono())
                    .addValue("TELEFONO_2",contacto.getTelefono2())
                    .addValue("CORREO",contacto.getCorreo())
                    .addValue("CORREO_2",contacto.getCorreo2())

                    .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                    .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                    .addValue("USUARIO", baseRequest.getUsername())
                    .addValue("ID_ROL", baseRequest.getIdRol())
                    .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades())

                    .addValue("FLAG_CONFIRMAR", contacto.getFlagConfirmar())
                    .addValue("ID_RQ", contacto.getIdRq());

            Map<String, Object> result = simpleJdbcCall.execute(params);

            List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");
            if (resultSet != null && !resultSet.isEmpty()) {
                Map<String, Object> row = resultSet.get(0);
                Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
                String mensaje = (String) row.get("MENSAJE");

                return new BaseResponse(idTipoMensaje, mensaje);
            }
            return new BaseResponse(3, "Ocurrió un problema al ejecutar el servicio");
        } catch (Exception e) {
            return new BaseResponse(3, "Error al conectarse a base datos");
        }
    }

    public BaseResponse updateContact(BaseRequest baseRequest, ContactUpdateRequest contacto) {
        try {
            SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("SP_CLIENTE_CONTACTO_UPD");

            SqlParameterSource params = new MapSqlParameterSource()
                    .addValue("ID_CLIENTE_CONTACTO", contacto.getIdClienteContacto())
                    .addValue("NOMBRES",contacto.getNombres())
                    .addValue("APELLIDO_PATERNO",contacto.getApellidoPaterno())
                    .addValue("APELLIDO_MATERNO",contacto.getApellidoMaterno())
                    .addValue("CARGO",contacto.getCargo())
                    .addValue("TELEFONO",contacto.getTelefono())
                    .addValue("TELEFONO_2",contacto.getTelefono2())
                    .addValue("CORREO",contacto.getCorreo())
                    .addValue("CORREO_2",contacto.getCorreo2())

                    .addValue("ID_USUARIO", baseRequest.getIdUsuario())
                    .addValue("ID_EMPRESA", baseRequest.getIdEmpresa())
                    .addValue("USUARIO", baseRequest.getUsername())
                    .addValue("ID_ROL", baseRequest.getIdRol())
                    .addValue("ID_FUNCIONALIDADES", baseRequest.getFuncionalidades());

            Map<String, Object> result = simpleJdbcCall.execute(params);

            List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");
            if (resultSet != null && !resultSet.isEmpty()) {
                Map<String, Object> row = resultSet.get(0);
                Integer idTipoMensaje = (Integer) row.get("ID_TIPO_MENSAJE");
                String mensaje = (String) row.get("MENSAJE");

                return new BaseResponse(idTipoMensaje, mensaje);
            }
            return new BaseResponse(3, "Ocurrió un problema al ejecutar el servicio");
        } catch (Exception e) {
            return new BaseResponse(3, "Error al conectarse a base datos");
        }
    }
}
