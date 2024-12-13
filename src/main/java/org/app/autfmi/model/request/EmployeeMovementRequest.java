package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeMovementRequest {
    private Integer idUsuarioTalento;
    private String unidad;
    private String empresa;
    private Double montoBase;
    private Double montoMovilidad;
    private Double montoTrimestral;
    private Double montoSemestral;
    private String puesto;
    private String area;
    private String jornada;
    private String fchMovimiento;
}
