package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.TalentDTO;

@Getter
@Setter
public class TalentResponse extends BaseResponse {
    private TalentDTO talent;

    public TalentResponse(Integer idTipoMensaje, String mensaje, TalentDTO talent) {
        super(idTipoMensaje, mensaje);
        this.talent = talent;
    }
}
