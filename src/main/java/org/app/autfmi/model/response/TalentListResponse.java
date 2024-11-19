package org.app.autfmi.model.response;

import lombok.*;
import org.app.autfmi.model.dto.TalentDTO;

import java.util.List;

@Getter
@Setter
public class TalentListResponse extends BaseResponse {
    private List<TalentDTO> talentos;

    public TalentListResponse(Integer idTipoMensaje, String mensaje, List<TalentDTO> talentos) {
        super(idTipoMensaje, mensaje);
        this.talentos = talentos;
    }
}
