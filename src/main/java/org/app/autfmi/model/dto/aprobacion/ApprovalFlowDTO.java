package org.app.autfmi.model.dto.aprobacion;

import java.util.List;

/** Permisos, versión y línea de tiempo de una solicitud (SP_FMI_SOLICITUD_APROBACION_FLUJO_SEL). */
public record ApprovalFlowDTO(
    String tipoSolicitud,
    String estado,
    /** Gestor asignado al cliente de la solicitud. */
    boolean esGestor,
    /** Puede editar y aprobar: gestor asignado y solicitud PENDIENTE. */
    boolean puedeGestionar,
    /** Versión leída; se devuelve al guardar o aprobar para detectar cambios ajenos. */
    Long version,
    List<ApprovalEditableFieldDTO> camposEditables,
    List<ApprovalTimelineEntryDTO> historial) {

  public ApprovalFlowDTO withCamposEditables(List<ApprovalEditableFieldDTO> campos) {
    return new ApprovalFlowDTO(tipoSolicitud, estado, esGestor, puedeGestionar, version, campos, historial);
  }
}
