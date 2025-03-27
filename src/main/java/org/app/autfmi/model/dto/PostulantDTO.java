package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostulantDTO {
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String celular;
    private String email;
    private String dni;
    private String tiempoContrato;
    private String fechaInicioLabores;
    private String cargo;
    private Double remuneracion;
    private String modalidad;
}
