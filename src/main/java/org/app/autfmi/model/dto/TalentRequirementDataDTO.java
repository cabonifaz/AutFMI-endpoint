package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TalentRequirementDataDTO {
    private Integer idTalento;
    private String nombres;
    private String apellidos;
    private String dni;
    private String celular;
    private String email;
    private Integer idSituacion;
    private String situacion;
    private String tooltip;
    private Integer idEstado;
    private String estado;
    private Integer tieneEquipo;
}
