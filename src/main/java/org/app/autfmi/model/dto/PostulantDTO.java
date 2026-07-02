package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostulantDTO {
    private String nombres;
    private String apellidos;
    private String celular;
    private String email;
    private String dni;
    private String tiempoContrato;
    private String fechaInicioLabores;
    private String cargo;
    private String remuneracion;
    private String modalidad;
    private String tieneEquipo;
}
