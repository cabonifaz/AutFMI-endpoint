package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TalentItemDTO {
    private Integer idUsuarioTalento;
    private Integer idTalento;
    private Boolean esTrabajador;
    private String nombres;
    private String apellidos;
    private String modalidad;
}
