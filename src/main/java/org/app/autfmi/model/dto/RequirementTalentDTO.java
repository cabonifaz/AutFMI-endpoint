package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementTalentDTO {
    private Integer idTalento;
    private String nombresTalento;
    private String apellidosTalento;
    private String dni;
    private String celular;
    private String email;
    private Integer idSituacion;
    private String situacion;
    private Integer idEstado;
    private String estado;
    private Integer idPerfil;
    private String perfil;
    private boolean confirmado;
    private String tooltip;
    private Integer tieneEquipo;
    private String ubicacion;
    private Integer idModalidadContrato;
    private String fchInicioContrato;
    private String fchTerminoContrato;
    private BigDecimal montoBase;
    private Integer idCvFile;
}
