package org.app.autfmi.model.response;

import lombok.*;
import org.app.autfmi.model.dto.TalentItemDTO;

import java.util.List;

@Getter
@Setter
public class TalentListResponse extends BaseResponse {
    private List<TalentItemDTO> talentos;
    private Integer totalElementos;

    public TalentListResponse(Integer idTipoMensaje, String mensaje, List<TalentItemDTO> talentos, Integer totalElementos) {
        super(idTipoMensaje, mensaje);
        this.talentos = talentos;
        this.totalElementos = totalElementos;
    }
}
