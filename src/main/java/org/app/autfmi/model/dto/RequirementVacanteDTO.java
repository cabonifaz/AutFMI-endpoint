package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementVacanteDTO {
    private Integer idRequerimientoVacante;
    private Integer idPerfil;
    private String perfilProfesional;
    private Integer cantidad;
}
