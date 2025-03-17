package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RequirementTalentRequest {
    private Integer idRequerimiento;
    private List<RequirementTalentRequestDTO> lstTalentos;
}
