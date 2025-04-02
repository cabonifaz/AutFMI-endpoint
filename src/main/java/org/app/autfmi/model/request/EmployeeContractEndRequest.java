package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeContractEndRequest {
    private Integer idUsuarioTalento;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private Integer idArea;
    private Integer idMotivo;
    private Integer idCliente;
    private String cliente;
    private String fchCese;
}
