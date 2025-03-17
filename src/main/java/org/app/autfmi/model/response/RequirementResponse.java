package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.RequirementDTO;

@Getter
@Setter
public class RequirementResponse extends BaseResponse {
    private RequirementDTO requerimiento;

    public RequirementResponse(Integer idTipoMensaje, String mensaje, RequirementDTO requerimiento) {
        super(idTipoMensaje, mensaje);
        this.requerimiento = requerimiento;
    }
}
