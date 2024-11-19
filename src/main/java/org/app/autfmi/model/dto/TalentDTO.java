package org.app.autfmi.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TalentDTO {
    private Boolean perteneceEmpresa;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String email;
    private String dni;
    private Integer numTiempoContrato;
    private String strTiempoContrato;
    private Integer idTiempoContrato;
    private String fechaInicioLabores;
    private String cargo;
    private Double remuneracion;
    private String moneda;
    private Integer idMoneda;
    private String modalidad;
    private Integer idModalidad;
    private String ubicacion;
}
