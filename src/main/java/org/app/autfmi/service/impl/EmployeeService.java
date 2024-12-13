package org.app.autfmi.service.impl;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.EmployeeEntryResponse;
import org.app.autfmi.repository.EmployeeRepository;
import org.app.autfmi.repository.HistoryRepository;
import org.app.autfmi.service.IEmployeeService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeService implements IEmployeeService {
    private final EmployeeRepository employeeRepository;
    private final HistoryRepository historyRepository;
    private final JwtHelper jwt;

    @Override
    public BaseResponse saveEmployeeEntry(String token, EmployeeEntryRequest request) {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = String.join(",", Constante.GUARDAR_USUARIO, Constante.INGRESAR_EMPLEADO);
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        EmployeeEntryResponse employeeEntryResponse = employeeRepository.saveEmployeeEntry(baseRequest, request);

        if (employeeEntryResponse.getIdTipoMensaje() == 2) {
            request.setIdUsuarioTalento(employeeEntryResponse.getIdUsuarioTalento());
            return historyRepository.registerEntry(baseRequest, request);
        }
        return employeeEntryResponse;
    }

    @Override
    public BaseResponse saveEmployeeMovement(String token, EmployeeMovementRequest request) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, "");
        return historyRepository.registerMovement(baseRequest, request);
    }

    @Override
    public BaseResponse saveEmployeeContractEnd(String token, EmployeeContractEndRequest request) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, "");
        return historyRepository.registerContractTermination(baseRequest, request);
    }
}
