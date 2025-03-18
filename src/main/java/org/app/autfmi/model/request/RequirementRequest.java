package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class RequirementRequest {
    private Integer idCliente;
    private String cliente;
    private String codigoRQ;
    private Date fechaSolicitud;
    private String descripcion;
    private Integer estado;
    private Integer vacantes;
    private List<FileRequest> lstArchivos;
}
