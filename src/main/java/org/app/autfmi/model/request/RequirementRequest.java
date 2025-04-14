package org.app.autfmi.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class RequirementRequest {
    @JsonProperty()
    private Integer idRequerimiento;
    private Integer idCliente;
    private String cliente;
    private String codigoRQ;
    private Date fechaSolicitud;
    private String descripcion;
    private Integer estado;
    private Boolean autogenRQ;
    private List<VacanteRequirement> lstVacantes;
    @JsonProperty()
    private List<FileRequest> lstArchivos;
}
