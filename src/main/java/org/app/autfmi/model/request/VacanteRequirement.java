package org.app.autfmi.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VacanteRequirement {
    private Integer idPerfil;
    private Integer cantidad;
    @JsonProperty()
    private Integer idEstado;
    @JsonProperty()
    private Integer idRequerimientoVacante;
}
