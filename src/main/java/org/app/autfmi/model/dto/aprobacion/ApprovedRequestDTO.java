package org.app.autfmi.model.dto.aprobacion;

/** Solicitud que acaba de aprobarse, con el formulario a ejecutar (SP_FMI_SOLICITUD_APROBACION_APR). */
public record ApprovedRequestDTO(
    Integer idSolicitud,
    String tipoSolicitud,
    Integer idCliente,
    Integer idTalento,
    String nombreTalento,
    String datosFormulario) {
}
