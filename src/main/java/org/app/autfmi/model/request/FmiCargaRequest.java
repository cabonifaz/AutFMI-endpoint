package org.app.autfmi.model.request;

import java.math.BigDecimal;

import lombok.Data;

/**
 * Registro del contrato de un colaborador a partir de su FMI ya emitido.
 *
 * El requerimiento es una referencia opcional: viaja a
 * TALENTO_CONTRATO.ID_RQ y a HISTORIAL.ID_RQ, y no cambia nada del RQ. Los
 * datos vienen del formulario pero pasan por pantalla, así que llegan ya
 * revisados.
 */
@Data
public class FmiCargaRequest {

    private Integer idTalento;

    /** De qué requerimiento salió el contrato. Null = contrato suelto. */
    private Integer idRequerimiento;
    /** Lo decide el usuario: un FMI antiguo entra como contrato terminado. */
    private Boolean activo;

    private Integer idArea;
    private String cargo;
    private Integer idModalidadContrato;
    private Integer idMotivo;
    private String horario;
    private String proyectoServicio;
    private String objetoContrato;
    private Integer declararSunat;
    private String sedeDeclarar;
    private String ubicacion;
    private String cliente;

    private Integer idMoneda;
    private BigDecimal montoBase;
    private BigDecimal montoMovilidad;
    private BigDecimal montoMensual;
    private BigDecimal montoTrimestral;
    private BigDecimal montoSemestral;

    /** yyyy-MM-dd */
    private String fchInicioContrato;
    private String fchTerminoContrato;
}
