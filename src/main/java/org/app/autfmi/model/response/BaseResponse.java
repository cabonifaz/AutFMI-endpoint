package org.app.autfmi.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse { // Super class
    private Integer idTipoMensaje;
    private String mensaje; // add result type object
    private String detalleMensaje;

    public BaseResponse(Integer idTipoMensaje, String mensaje) {
        this.idTipoMensaje = idTipoMensaje;
        this.mensaje = mensaje;
    }

}
