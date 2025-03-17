package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementItemDTO {
    private Integer idRequerimiento;
    private String cliente;
    private String codigoRQ;
    private String fechaSolicitud;
    private String Estado;
    private Integer Vacantes;
}
