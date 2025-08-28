package org.app.autfmi.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.app.autfmi.model.dto.SolicitudEquipoDTO;

import java.math.BigDecimal;
import java.util.List;

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

    @JsonProperty()
    private Integer ingreso;
    @JsonProperty()
    private Integer idCliente;
    @JsonProperty()
    private String cliente;
    @JsonProperty()
    private Integer idArea;
    @JsonProperty()
    private String area;
    @JsonProperty()
    private String cargo;
    @JsonProperty()
    private String fchInicioContrato;
    @JsonProperty()
    private String fchTerminoContrato;
    @JsonProperty()
    private String proyectoServicio;
    @JsonProperty()
    private String objetoContrato;
    @JsonProperty()
    private Integer idModalidadContrato;
    @JsonProperty()
    private String horario;
    @JsonProperty()
    private Integer tieneEquipo;
    @JsonProperty()
    private String ubicacion;
    @JsonProperty()
    private Integer idMotivo;
    @JsonProperty()
    private Integer idMoneda;
    @JsonProperty()
    private Integer declararSunat;
    @JsonProperty()
    private String sedeDeclarar;
    @JsonProperty()
    private BigDecimal montoBase;
    @JsonProperty()
    private BigDecimal montoMovilidad;
    @JsonProperty()
    private BigDecimal montoTrimestral;
    @JsonProperty()
    private BigDecimal montoSemestral;

    @JsonProperty()
    private SolicitudEquipoDTO solicitudEquipo;
}
