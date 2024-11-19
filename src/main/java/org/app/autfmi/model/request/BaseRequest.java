package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseRequest {
    private Integer idUsuario;
    private Integer idEmpresa;
    private String tipoRol;
    private String funcionalidades;
}
