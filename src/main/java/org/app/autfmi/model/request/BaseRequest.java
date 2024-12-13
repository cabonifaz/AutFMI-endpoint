package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseRequest {
    private Integer idUsuario;
    private String username;
    private Integer idEmpresa;
    private Integer idRol;
    private String funcionalidades;
}
