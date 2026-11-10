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
    List<ApprovalTimelineEntryDTO> historial,
    boolean prontoInicioDisponible,
    int notificarProntoInicio) {

  public ApprovalFlowDTO(String tipoSolicitud, String estado, boolean esGestor, boolean puedeGestionar,
      Long version, List<ApprovalEditableFieldDTO> camposEditables, List<ApprovalTimelineEntryDTO> historial) {
    this(tipoSolicitud, estado, esGestor, puedeGestionar, version, camposEditables, historial, false, 0);
  }

  public ApprovalFlowDTO withInicioPreference(boolean disponible, int notificar) {
    return new ApprovalFlowDTO(tipoSolicitud, estado, esGestor, puedeGestionar, version, camposEditables,
        historial, disponible, notificar);
  }

  public ApprovalFlowDTO withCamposEditables(List<ApprovalEditableFieldDTO> campos) {
    return new ApprovalFlowDTO(tipoSolicitud, estado, esGestor, puedeGestionar, version, campos, historial, prontoInicioDisponible, notificarProntoInicio);
  }
}
