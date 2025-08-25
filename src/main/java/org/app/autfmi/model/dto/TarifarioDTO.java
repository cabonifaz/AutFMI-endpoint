package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TarifarioDTO {
    private Integer idPerfil;
    private String perfil;
    private BigDecimal tarifa;
    private String moneda;
    private String tipoTarifa;
}
