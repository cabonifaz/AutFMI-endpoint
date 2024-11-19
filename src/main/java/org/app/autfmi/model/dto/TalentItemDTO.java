package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TalentItemDTO {
    private Integer idTalento;
    private Boolean perteneceEmpresa;
    private String nombres;
    private String apellidos;
    private String modalidad;
}
