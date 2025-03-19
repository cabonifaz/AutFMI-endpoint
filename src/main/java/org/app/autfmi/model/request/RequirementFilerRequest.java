package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RequirementFilerRequest {
    private Integer idRequerimiento;
    private List<FileRequest> lstArchivos;
}
