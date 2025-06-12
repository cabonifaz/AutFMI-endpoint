package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.app.autfmi.model.response.BaseResponse;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntryReport implements IReport {
    private BaseResponse response;
    private String nombres;
    private String apellidos;
    private String unidad;
    private String fechaHistorial;
    private String modalidad;
    private String motivo;
    private String cargo;
    private String horario;
    private Double montoBase;
    private Double montoMovilidad;
    private Double montoTrimestral;
    private String fechaInicioContrato;
    private String fechaFinContrato;
    private String proyectoServicio;
    private String objetoContrato;
    private Integer declararSunat;
    private String sedeDeclararSunat;
    private String correoGestor;
    private String firmante;
    private String firma;
    private String usernameEmpleado;
    private String emailEmpleado;
}
