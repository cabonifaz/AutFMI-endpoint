package org.app.autfmi.model.dto;

import java.math.BigDecimal;

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
    private Integer totalHabilidades;
    private Integer totalCarreras;
    private BigDecimal tarifaInicial;
    private BigDecimal tarifaFinal;
}
