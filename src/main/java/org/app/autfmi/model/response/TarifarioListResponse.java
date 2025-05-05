package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.TarifarioDTO;

import java.util.List;

@Getter
@Setter
public class TarifarioListResponse extends BaseResponse{
    private List<TarifarioDTO> lstTarifario;

    public TarifarioListResponse(Integer idTipoMensaje, String mensaje, List<TarifarioDTO> lstTarifario) {
        super(idTipoMensaje, mensaje);
        this.lstTarifario = lstTarifario;
    }
}
