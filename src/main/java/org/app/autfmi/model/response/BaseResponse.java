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
}
