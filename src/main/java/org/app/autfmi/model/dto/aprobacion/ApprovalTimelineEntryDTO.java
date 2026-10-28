package org.app.autfmi.model.dto.aprobacion;

/** Un evento de la línea de tiempo (FMI_SOLICITUD_APROBACION_HIST). */
public record ApprovalTimelineEntryDTO(
    Integer idHistorial,
    String accion,
    String estadoAnterior,
    String estadoNuevo,
    String detalle,
    String usuario,
    /** yyyy-MM-dd HH:mm:ss */
    String fecha) {
}
