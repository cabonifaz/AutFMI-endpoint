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
    private Integer idCliente;
    private String cliente;
    private String codigoRQ;
    private Date fechaSolicitud;
    private String descripcion;
    private Integer idEstado;
    private String estado;
    private Integer vacantes;
    private Integer idDuracion;
    private String duracion;
    private Date fechaVencimiento;
    private Integer idModalidad;
    private String modalidad;
    private List<RequirementVacanteDTO> lstRqVacantes;
    private List<RequirementTalentDTO> lstRqTalento;
    private List<RequirementFileDTO> lstRqArchivo;
}
