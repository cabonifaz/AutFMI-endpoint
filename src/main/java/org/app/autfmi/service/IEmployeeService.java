package org.app.autfmi.service;

import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.app.autfmi.model.response.BaseResponse;

public interface IEmployeeService {
    BaseResponse saveEmployeeEntry(String token, EmployeeEntryRequest request);
    BaseResponse saveEmployeeMovement(String token, EmployeeMovementRequest request);
    BaseResponse saveEmployeeContractEnd(String token, EmployeeContractEndRequest request);
}
