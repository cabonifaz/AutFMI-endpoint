package org.app.autfmi.model.response;

import java.util.List;

import org.app.autfmi.model.dto.VacanteSkillDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VacanteSkillsResponse extends BaseResponse {

    private List<VacanteSkillDTO> habilidades;

    public VacanteSkillsResponse(Integer idTipoMensaje, String mensaje, List<VacanteSkillDTO> habilidades) {
        super(idTipoMensaje, mensaje);
        this.habilidades = habilidades;
    }

}
