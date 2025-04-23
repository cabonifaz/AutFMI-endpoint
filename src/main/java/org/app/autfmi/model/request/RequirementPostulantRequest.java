package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RequirementPostulantRequest {
    private int idRQ;
    private int idPerfil;
}
