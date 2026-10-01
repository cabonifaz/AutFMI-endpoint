package org.app.autfmi.model.response;

import java.util.List;

import org.app.autfmi.model.dto.TalentMatchDTO;

import lombok.Getter;
import lombok.Setter;

/** Candidatos a "esta persona ya existe" para la carga desde un FMI. */
@Getter
@Setter
public class TalentMatchListResponse extends BaseResponse {

    private List<TalentMatchDTO> talentos;

    public TalentMatchListResponse(Integer idTipoMensaje, String mensaje, List<TalentMatchDTO> talentos) {
        super(idTipoMensaje, mensaje);
        this.talentos = talentos;
    }
}
