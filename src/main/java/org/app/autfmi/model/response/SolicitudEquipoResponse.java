package org.app.autfmi.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEquipoResponse {
    private BaseResponse baseResponse;
    private String correoGestor;
    private String nombres;
    private String apellidos;
}
