package org.app.autfmi.service;

import java.util.List;
import java.util.function.Supplier;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.RequirementTalentsResult;
import org.app.autfmi.model.dto.TipoFormulario;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.EmailConfirmationRequest;
import org.app.autfmi.model.response.BaseResponse;

/**
 * Confirmación del gestor o suplente del cliente antes de enviar cualquier
 * formulario (Asignar Talento e ingreso, movimiento, cese y solicitud de equipo
 * de FMI). Si quien genera el formulario ya es gestor o suplente del cliente,
 * se envía directo.
 */
public interface IRequirementConfirmationService {

    enum ResultadoSolicitud {
        /** Quien generó el formulario es gestor/suplente del cliente (o no hay
         *  cliente): el llamador envía los formularios directo. */
        DIRECTO,
        /** Correo de confirmación enviado al gestor/suplente. */
        ENVIADA,
        /** El cliente no tiene gestor ni suplente con correo válido: pendiente. */
        SIN_GESTORES,
        /** Falló el registro o el envío del correo de confirmación: pendiente. */
        ERROR_ENVIO
    }

    /**
     * Asignar Talento (Finalizar). Los talentos ya quedaron con
     * ESTADO_FORMULARIOS 0 (o 1 si el actor es gestor) en el SP.
     *
     * @param baseRequest usuario que finalizó; con él se generan los formularios
     *                    que se adjuntan al correo de confirmación
     */
    ResultadoSolicitud requestConfirmation(Integer idRequerimiento, RequirementTalentsResult result,
            Integer idUsuarioActor, BaseRequest baseRequest);

    /**
     * Formularios de las pantallas de FMI. Registra el formulario y, si
     * corresponde, envía el correo de confirmación.
     *
     * @param id ID_HISTORIAL / ID_EQUIPO_SOLICITUD (null en INGRESO: lo resuelve el SP)
     * @param idCliente cliente cuyo gestor confirma (en MOVIMIENTO, el de destino)
     * @param talento nombre del talento para el correo
     * @param adjuntos PDFs del formulario para adjuntar al correo de confirmación;
     *                 solo se generan si de verdad se pide confirmación
     */
    ResultadoSolicitud requestFormConfirmation(TipoFormulario tipo, Integer id, Integer idTalento,
            Integer idCliente, Integer idUsuarioActor, String talento, Supplier<List<FileDTO>> adjuntos);

    /** Texto para detalleMensaje según el resultado; null en DIRECTO. */
    String describeResultado(ResultadoSolicitud resultado);

    /**
     * Procesa la confirmación que llega por correo (vía n8n): valida, reclama lo
     * pendiente, envía los formularios y marca el resultado.
     */
    BaseResponse processEmailConfirmation(EmailConfirmationRequest request);
}
