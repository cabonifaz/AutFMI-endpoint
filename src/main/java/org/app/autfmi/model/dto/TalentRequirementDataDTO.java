package org.app.autfmi.model.dto;

import java.math.BigDecimal;

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
    private BigDecimal montoInicialPlanilla;
    private BigDecimal montoFinalPlanilla;
    private BigDecimal montoInicialRxH;
    private BigDecimal montoFinalRxH;
    private Integer idMonedaPlan;
    private Integer idMonedaRxh;
    private Integer idModalidadFacturacion;
}
