package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VacanteCarreraRequest {
    private Integer idPerfil;
    private String carrera;
    private Integer idGrado;
}
