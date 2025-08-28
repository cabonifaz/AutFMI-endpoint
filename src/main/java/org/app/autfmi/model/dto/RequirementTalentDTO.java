package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementTalentDTO {
    private Integer idTalento;
    private String nombresTalento;
    private String apellidosTalento;
    private String dni;
    private String celular;
    private String email;
    private Integer idSituacion;
    private String situacion;
    private Integer idEstado;
    private String estado;
    private Integer idPerfil;
    private String perfil;
    private boolean confirmado;
    private String tooltip;
    private Integer tieneEquipo;
}
