package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VacanteSkill {

    private int idPerfil;
    private int idSkill;
    private int anios;
    private Boolean isOptional;
}
