package org.app.autfmi.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContactRegisterRequest {
    private int idCliente;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String cargo;
    private String telefono;
    private String telefono2;
    private String correo;
    private String correo2;
    private int flagConfirmar;
    @JsonProperty()
    private int idRq;
}
