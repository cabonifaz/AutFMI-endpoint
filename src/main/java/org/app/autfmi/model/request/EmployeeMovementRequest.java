package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeMovementRequest {
    private Integer idUsuarioTalento;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private Integer idArea;
    private Integer idMoneda;
    private Integer idModalidad;
    private String fchInicioContrato;
    private String fchTerminoContrato;
    private String proyectoServicio;
    private String objetoContrato;
    private Integer idCliente;
    private String cliente;
    private Double montoBase;
    private Double montoMovilidad;
    private Double montoTrimestral;
    private Double montoSemestral;
    private String puesto;
    private Integer idMovArea;
    private String jornada;
    private String fchMovimiento;
}
