package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeEntryRequest {
    private Integer idRequerimiento;
    /** FTBO-1129: null conserva la preferencia del contrato. */
    private Integer notificarProntoInicio;
    /** Instantánea de la selección; conserva perfil y solicitud de equipo. */
    private RequirementTalentRequestDTO seleccion;
    // Employee record
    private Integer idTalento;
    private Integer idArea;
    private String cargo;
    private String horario;
    private Double remuneracion;
    private String fchInicioContrato;
    private String fchTerminoContrato;
    private String proyectoServicio;
    private String objetoContrato;
    private Integer idTiempoContrato;
    private Integer tiempoContrato;
    private Integer idModalidadContrato;
    private Integer tieneEquipo;
    private String ubicacion;
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
