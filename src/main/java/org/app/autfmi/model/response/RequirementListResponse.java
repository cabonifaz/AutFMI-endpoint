package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.RequirementItemDTO;

import java.util.List;

@Getter
@Setter
public class RequirementListResponse extends BaseResponse{
    private List<RequirementItemDTO> requerimientos;

    public RequirementListResponse(Integer idTipoMensaje, String mensaje, List<RequirementItemDTO> requerimientos) {
        super(idTipoMensaje, mensaje);
        this.requerimientos = requerimientos;
    }
}
