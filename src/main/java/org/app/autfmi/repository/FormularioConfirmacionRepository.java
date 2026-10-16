package org.app.autfmi.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.app.autfmi.model.dto.FormularioConfirmacionDTO;
import org.app.autfmi.model.dto.GestorClienteDTO;
import org.app.autfmi.model.dto.TipoFormulario;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Confirmación de formularios generados desde las pantallas de FMI (ingreso,
 * movimiento, cese y solicitud de equipo). El de Asignar Talento vive en
 * {@link RequirementRepository}.
 */
@SuppressWarnings("unchecked")
@Repository
@RequiredArgsConstructor
public class FormularioConfirmacionRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Registra el formulario: pendiente (0) si quien lo generó no es gestor ni
     * suplente del cliente; enviado (1) si lo es. {@code id} puede ser null en
     * INGRESO (el SP toma el último ingreso del talento registrado por el usuario).
     */
    public FormularioConfirmacionDTO.Registro registrar(TipoFormulario tipo, Integer id, Integer idTalento,
            Integer idCliente, Integer idUsuario) {
        var params = new MapSqlParameterSource()
                .addValue("TIPO", tipo.name())
                .addValue("ID", id)
                .addValue("ID_TALENTO", idTalento)
                .addValue("ID_CLIENTE", idCliente)
                .addValue("ID_USUARIO", idUsuario);

        Map<String, Object> result = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_FORMULARIO_CONFIRMACION_REGISTRAR")
                .execute(params);

        var registro = new FormularioConfirmacionDTO.Registro();
        var rs1 = (List<Map<String, Object>>) result.get("#result-set-1");
        if (rs1 == null || rs1.isEmpty()) {
            registro.setBaseResponse(new BaseResponse(3, "No se obtuvo respuesta de la base de datos"));
            return registro;
        }

        var row = rs1.get(0);
        var messageId = (Integer) row.getOrDefault("ID_TIPO_MENSAJE", 3);
        registro.setBaseResponse(new BaseResponse(messageId, (String) row.get("MENSAJE")));
        if (messageId != 2) {
            return registro;
        }

        registro.setIdFormulario((Integer) row.get("ID_FORMULARIO"));
        registro.setEsGestor(Boolean.TRUE.equals(row.get("ES_GESTOR")));
        registro.setCliente((String) row.get("CLIENTE"));
        registro.setSolicitante((String) row.get("SOLICITANTE"));
        registro.setCorreoSolicitante((String) row.get("CORREO_SOLICITANTE"));
        registro.setGestores(mapGestores((List<Map<String, Object>>) result.get("#result-set-2")));
        return registro;
    }

    /**
     * Valida remitente (gestor/suplente del cliente del formulario) y reclama el
     * formulario (0 -> 2). Con éxito queda en 2 hasta llamar a {@link #marcar}.
     */
    public FormularioConfirmacionDTO.Proceso procesar(TipoFormulario tipo, Integer id, String emailRemitente) {
        var params = new MapSqlParameterSource()
                .addValue("TIPO", tipo.name())
                .addValue("ID", id)
                .addValue("EMAIL_REMITENTE", emailRemitente);

        Map<String, Object> result = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_FORMULARIO_CONFIRMACION_PROCESAR")
                .execute(params);

        var proceso = new FormularioConfirmacionDTO.Proceso();
        var rs1 = (List<Map<String, Object>>) result.get("#result-set-1");
        if (rs1 == null || rs1.isEmpty()) {
            proceso.setBaseResponse(new BaseResponse(3, "No se obtuvo respuesta de la base de datos"));
            return proceso;
        }

        var row = rs1.get(0);
        var messageId = (Integer) row.getOrDefault("ID_TIPO_MENSAJE", 3);
        proceso.setBaseResponse(new BaseResponse(messageId, (String) row.get("MENSAJE")));
        if (messageId != 2) {
            return proceso;
        }

        var rs2 = (List<Map<String, Object>>) result.get("#result-set-2");
        if (rs2 != null && !rs2.isEmpty()) {
            var datos = rs2.get(0);
            proceso.setIdTalento((Integer) datos.get("ID_TALENTO"));
            proceso.setBaseRequestGenera(new BaseRequest(
                    (Integer) datos.get("ID_USUARIO"),
                    (String) datos.get("USUARIO"),
                    (Integer) datos.get("ID_EMPRESA"),
                    (Integer) datos.get("ID_ROL"),
                    tipo.getFuncionalidades()));
        }

        var confirmador = mapGestores((List<Map<String, Object>>) result.get("#result-set-3"));
        if (!confirmador.isEmpty()) {
            proceso.setConfirmador(confirmador.get(0));
        }
        return proceso;
    }

    /** Cierra el reclamo: 2 -> 1 si el envío salió, 2 -> 0 si falló. */
    public BaseResponse marcar(TipoFormulario tipo, Integer id, boolean exito) {
        var params = new MapSqlParameterSource()
                .addValue("TIPO", tipo.name())
                .addValue("ID", id)
                .addValue("EXITO", exito);

        Map<String, Object> result = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_FORMULARIO_CONFIRMACION_MARCAR")
                .execute(params);

        var rs1 = (List<Map<String, Object>>) result.get("#result-set-1");
        if (rs1 == null || rs1.isEmpty()) {
            return new BaseResponse(3, "No se obtuvo respuesta de la base de datos");
        }
        var row = rs1.get(0);
        return new BaseResponse((Integer) row.getOrDefault("ID_TIPO_MENSAJE", 3), (String) row.get("MENSAJE"));
    }

    private List<GestorClienteDTO> mapGestores(List<Map<String, Object>> rows) {
        List<GestorClienteDTO> gestores = new ArrayList<>();
        if (rows == null) {
            return gestores;
        }
        for (var row : rows) {
            gestores.add(new GestorClienteDTO(
                    (Integer) row.get("PRIORIDAD"),
                    (Integer) row.get("ID_USUARIO"),
                    (String) row.get("NOMBRES"),
                    (String) row.get("APELLIDOS"),
                    (String) row.get("CORREO")));
        }
        return gestores;
    }
}
