package org.app.autfmi.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse extends BaseResponse {
    private String token;

    public AuthResponse(Integer idTipoMensaje, String mensaje, String token) {
        super(idTipoMensaje, mensaje);
        this.token = token;
    }
}
