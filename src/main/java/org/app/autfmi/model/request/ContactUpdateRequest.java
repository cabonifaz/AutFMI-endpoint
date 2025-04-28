package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContactUpdateRequest {
    private int idClienteContacto;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String cargo;
    private String telefono;
    private String telefono2;
    private String correo;
    private String correo2;
}
