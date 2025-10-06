package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VacanteSkillDTO {

    private Integer idVacanteHabilidad;
    private Integer idVacante;
    private Integer idPerfil;
    private Integer idHabilidad;
    private String habilidad;
    private Integer idEstadoRegistro;
    private Integer anios;
}