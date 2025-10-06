package org.app.autfmi.model.response;

import java.util.List;

import org.app.autfmi.model.dto.VacanteCarreraDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VacanteCarreraResponse extends BaseResponse {

    private List<VacanteCarreraDTO> carreras;

    public VacanteCarreraResponse(Integer idTipoMensaje, String mensaje, List<VacanteCarreraDTO> carreas) {
        super(idTipoMensaje, mensaje);
        this.carreras = carreas;
    }

}
