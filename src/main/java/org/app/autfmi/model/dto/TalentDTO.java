package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TalentDTO {
    private Integer idTalento;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String email;
    private String dni;
    private Integer numTiempoContrato;
    private String strTiempoContrato;
    private String fechaInicioLabores;
    private String cargo;
    private Double remuneracion;
    private String moneda;
    private String modalidad;
    private String ubicacion;
}
