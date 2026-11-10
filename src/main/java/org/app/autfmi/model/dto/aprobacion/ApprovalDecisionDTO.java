package org.app.autfmi.model.dto.aprobacion;

/** Resultado de guardar o aprobar: estado y versión vigentes de la solicitud. */
public record ApprovalDecisionDTO(Integer idSolicitud, String estado, Long version) {
}
