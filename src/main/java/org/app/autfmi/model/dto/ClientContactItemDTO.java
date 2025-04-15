package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientContactItemDTO {
    private Integer idClienteContacto;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String cargo;
    private String telefono;
    private String correo;
    private Integer idEstadoContacto;
}
