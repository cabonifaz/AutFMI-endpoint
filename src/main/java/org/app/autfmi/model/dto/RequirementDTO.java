package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementDTO {
    private Integer idCliente;
    private String cliente;
    private String titulo;
    private String codigoRQ;
    private String fechaSolicitud;
    private String descripcion;
    private Integer idEstado;
    private String estado;
    private Integer vacantes;
    private Integer idDuracion;
    private BigDecimal duracion;
    private String fechaVencimiento;
    private Integer idModalidad;
    private String modalidad;
    private List<RequirementVacanteDTO> lstRqVacantes;
    private List<RequirementTalentDTO> lstRqTalento;
    private List<RequirementFileDTO> lstRqArchivo;
    private List<ClientContactItemDTO> lstRqContactos;
}
