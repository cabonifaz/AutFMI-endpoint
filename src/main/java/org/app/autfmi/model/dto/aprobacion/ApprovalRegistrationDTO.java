package org.app.autfmi.model.dto.aprobacion;

import java.util.List;

/** Solicitud recién registrada y los gestores a notificar. */
public record ApprovalRegistrationDTO(Integer idSolicitud, String cliente, List<ApprovalGestorDTO> gestores) {
}
