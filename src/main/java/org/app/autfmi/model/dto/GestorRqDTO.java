package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GestorRqDTO {
    private String nombres;
    private String apellidos;
    private String correo;

    private String cliente;
    private String tipoFormulario;
    private String tieneEquipo;
}
