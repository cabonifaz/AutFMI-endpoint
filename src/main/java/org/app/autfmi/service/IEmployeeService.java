package org.app.autfmi.service;

import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.response.BaseResponse;

public interface IEmployeeService {
    BaseResponse saveEmployee(String token, EmployeeEntryRequest request);
}
