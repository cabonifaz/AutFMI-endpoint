package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.app.autfmi.model.response.BaseResponse;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovementReport implements IReport {
    private BaseResponse response;
    private String nombres;
    private String apellidos;
    private String unidad;
    private String puesto;
    private String area;
    private String horario;
    private String fechaHistorial;
    private Double montoBase;
    private Double montoMovilidad;
    private Double montoTrimestral;
    private String correoGestor;
    private String firmante;
    private String firma;
    private String usernameEmpleado;
    private String emailEmpleado;
}
