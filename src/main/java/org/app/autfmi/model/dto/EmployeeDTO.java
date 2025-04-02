package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDTO {
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private Integer idArea;
    private Double remuneracion;
    private Integer idCliente;
    private String cargo;
}
