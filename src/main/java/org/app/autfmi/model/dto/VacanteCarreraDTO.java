package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VacanteCarreraDTO {
    private Integer idVacanteCarrera;
    private Integer idVacante;
    private Integer idPerfil;
    private String carrera;
    private Integer idGradoEstudios;
    private Integer idEstadoRegistro;
    private Boolean isOptional;
}
