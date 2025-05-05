package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RequirementTalentRequest {
    private Integer idRequerimiento;
    private Boolean flagCorreo;
    private List<RequirementTalentRequestDTO> lstTalentos;
}
