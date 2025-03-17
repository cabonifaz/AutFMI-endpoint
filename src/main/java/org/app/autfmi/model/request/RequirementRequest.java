package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class RequirementRequest {
    private String cliente;
    private String codigoRQ;
    private Date fechaSolicitud;
    private String descripcion;
    private Integer estado;
    private Integer vacantes;
}
