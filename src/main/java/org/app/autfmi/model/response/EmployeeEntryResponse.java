package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeEntryResponse extends BaseResponse {
    private Integer idUsuarioTalento;

    public EmployeeEntryResponse(Integer idTipoMensaje, String mensaje, Integer idUsuarioTalento) {
        super(idTipoMensaje, mensaje);
        this.idUsuarioTalento = idUsuarioTalento;
    }
}
