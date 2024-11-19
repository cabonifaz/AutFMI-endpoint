package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.TalentDTO;

@Getter
@Setter
public class TalentResponse extends BaseResponse {
    private TalentDTO talento;

    public TalentResponse(Integer idTipoMensaje, String mensaje, TalentDTO talento) {
        super(idTipoMensaje, mensaje);
        this.talento = talento;
    }
}
