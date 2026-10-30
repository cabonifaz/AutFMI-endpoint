package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.app.autfmi.model.dto.RequirementTalentsResult;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.BaseResponse;

/** Conserva todos los formularios de Selección, incluida la solicitud de equipo. */
@Data
@AllArgsConstructor
public class SelectionEntryReport implements IReport {
    private BaseResponse response;
    private RequirementTalentsResult result;
    private BaseRequest actor;
}
