package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementDTO {
    private String cliente;
    private String codigoRQ;
    private Date fechaSolicitud;
    private String descripcion;
    private Integer Estado;
    private Integer Vacantes;
    private List<RequirementTalentDTO> lstRqTalento;
    private List<RequirementFileDTO> lstRqArchivo;
}
