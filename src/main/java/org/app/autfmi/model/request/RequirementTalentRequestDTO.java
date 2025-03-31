package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RequirementTalentRequestDTO {
    private Integer idTalento;
    private String nombres;
    private String apellidos;
    private String dni;
    private String celular;
    private String email;
    private Integer idSituacion;
    private Integer idEstado;
    private boolean confirmado;
}
