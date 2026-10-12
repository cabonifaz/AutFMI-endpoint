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
    private String codigoRQ;
    private String cliente;
    private String tipoFormulario;
    // Requisitos de ingreso del RQ (0 = No requiere, 1 = Requiere)
    private Integer emo;
    private Integer sctr;
    private Integer asignacionLaptop;
}
