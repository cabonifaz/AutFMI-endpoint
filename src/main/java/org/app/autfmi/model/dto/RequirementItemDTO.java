package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementItemDTO {
    private Integer idRequerimiento;
    private String cliente;
    private String codigoRQ;
    private String fechaSolicitud;
    private Integer idEstado;
    private String estado;
    private Integer vacantes;
    private String duracion;
    private String fechaVencimiento;
    private String modalidad;
    private Integer idAlerta;
    private List<RequirementPerfilItemDTO> lstPerfiles;
}
