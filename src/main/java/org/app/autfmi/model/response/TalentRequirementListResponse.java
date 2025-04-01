package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.TalentRequirementItemDTO;

import java.util.List;

@Getter
@Setter
public class TalentRequirementListResponse extends BaseResponse {
    private List<TalentRequirementItemDTO> talentos;

    public TalentRequirementListResponse(Integer idTipoMensaje, String mensaje, List<TalentRequirementItemDTO> talentos) {
        super(idTipoMensaje, mensaje);
        this.talentos = talentos;
    }
}
