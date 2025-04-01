package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeEntryRequest {
    // Employee record
    private Integer idTalento;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private Integer idUsuarioTalento;
    private Integer idArea;
    private String cargo;
    private String horarioTrabajo;
    private String fchInicioContrato;
    private String fchTerminoContrato;
    private String proyectoServicio;
    private String objetoContrato;
    // History record
    private Integer idModalidad;
    private Integer idMotivo;
    private Integer idMoneda;
    private Integer idCliente;
    private String cliente;
    private Double montoBase;
    private Double montoMovilidad;
    private Double montoTrimestral;
    private Double montoSemestral;
    private Integer declararSunat;
    private String sedeDeclarar;
    private String fchHistorial;
}
