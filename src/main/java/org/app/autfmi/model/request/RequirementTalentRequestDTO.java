package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RequirementTalentRequestDTO {
    private Integer idTalento;
    private String nombres;
    private String apellidos;
    private String dni;
    private String celular;
    private String email;
    private Integer idSituacion;
    private Integer idEstado;
    private Integer idPerfil;
    private boolean confirmado;

    private Integer ingreso;
    private Integer idCliente;
    private Integer idArea;
    private String cargo;
    private String fchInicioContrato;
    private String fchTerminoContrato;
    private String proyectoServicio;
    private String objetoContrato;
    private Integer idModalidadContrato;
    private String horario;
    private Integer tieneEquipo;
    private String ubicacion;
    private Integer idMotivo;
    private Integer idMoneda;
    private Integer declararSunat;
    private String sedeDeclarar;
    private Double montoBase;
    private Double montoMovilidad;
    private Double montoTrimestral;
    private Double montoSemestral;
}
