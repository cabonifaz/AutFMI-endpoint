package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.EmployeeDTO;

@Getter
@Setter
public class EmployeeResponse extends BaseResponse {
    private EmployeeDTO employee;

    public EmployeeResponse(Integer idTipoMensaje, String mensaje, EmployeeDTO employee) {
        super(idTipoMensaje, mensaje);
        this.employee = employee;
    }
}
