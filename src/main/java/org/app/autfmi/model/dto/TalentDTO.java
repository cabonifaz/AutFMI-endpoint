package org.app.autfmi.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TalentDTO {
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String celular;
    private String email;
    private String dni;
    private String cargo;
    private Double remuneracion;
    private Integer idMoneda;
}
