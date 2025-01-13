package org.app.autfmi.service;

import jakarta.mail.MessagingException;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.FilePDFResponse;

public interface IEmployeeService {
    BaseResponse getEmployee(Integer idUsuarioTalento);
    BaseResponse saveEmployeeEntry(String token, EmployeeEntryRequest request) throws MessagingException;
    BaseResponse saveEmployeeMovement(String token, EmployeeMovementRequest request) throws MessagingException;
    BaseResponse saveEmployeeContractEnd(String token, EmployeeContractEndRequest request) throws MessagingException;
    FilePDFResponse getLastHistory(String token, Integer idTipoHistorial, Integer idUsuarioTalento);
}
