package org.app.autfmi.model.dto;

import java.util.List;

import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.BaseResponse;

import lombok.Data;

/**
 * Resultados de los SP de confirmación de formularios generados desde FMI
 * (SP_FORMULARIO_CONFIRMACION_REGISTRAR / _PROCESAR).
 */
public class FormularioConfirmacionDTO {

    private FormularioConfirmacionDTO() {
    }

    /** SP_FORMULARIO_CONFIRMACION_REGISTRAR */
    @Data
    public static class Registro {
        private BaseResponse baseResponse;
        private Integer idFormulario;
        /** Quien generó el formulario es gestor/suplente del cliente: se envía directo. */
        private boolean esGestor;
        private String cliente;
        private String solicitante;
        private String correoSolicitante;
        private List<GestorClienteDTO> gestores;
    }

    /** SP_FORMULARIO_CONFIRMACION_PROCESAR */
    @Data
    public static class Proceso {
        private BaseResponse baseResponse;
        private Integer idTalento;
        /** Usuario que generó el formulario: los SP de reporte validan sus permisos. */
        private BaseRequest baseRequestGenera;
        private GestorClienteDTO confirmador;
    }
}
