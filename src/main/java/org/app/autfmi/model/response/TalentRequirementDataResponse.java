package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.TalentRequirementDataDTO;

@Getter
@Setter
public class TalentRequirementDataResponse extends BaseResponse{
    public TalentRequirementDataDTO talento;
    public TalentRequirementDataResponse(Integer idTipoMensaje, String mensaje, TalentRequirementDataDTO talento) {
        super(idTipoMensaje, mensaje);
        this.talento = talento;
    }

}
