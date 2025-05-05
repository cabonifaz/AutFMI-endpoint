package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementPerfilItemDTO {
    private int idPerfil;
    private String perfil;
    private int vacantesTotales;
    private int vacantesCubiertas;
}
