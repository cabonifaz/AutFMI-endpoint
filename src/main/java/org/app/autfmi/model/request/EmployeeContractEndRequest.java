package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeContractEndRequest {
    private Integer idUsuarioTalento;
    private String nombres;
    private String apellidos;
    private Integer idUnidad;
    private Integer idMotivo;
    private String empresa;
    private String fchCese;
}
