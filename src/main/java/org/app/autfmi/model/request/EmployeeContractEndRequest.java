package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeContractEndRequest {
    private Integer idUsuarioTalento;
    private String unidad;
    private String empresa;
    private String motivoCese;
    private String fchCese;
}
