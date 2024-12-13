package org.app.autfmi.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.service.impl.EmployeeService;
import org.app.autfmi.util.JwtHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("fmi/employee")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @PostMapping("/entry")
    public ResponseEntity<BaseResponse> saveEmployeeEntry(
            @RequestBody EmployeeEntryRequest employeeEntryRequest,
            HttpServletRequest httpServletRequest
    ) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = employeeService.saveEmployeeEntry(token, employeeEntryRequest);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/movement")
    public ResponseEntity<BaseResponse> saveEmployeeMovement(
            @RequestBody EmployeeMovementRequest employeeMovementRequest,
            HttpServletRequest httpServletRequest
    ) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = employeeService.saveEmployeeMovement(token, employeeMovementRequest);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/contractTermination")
    public ResponseEntity<BaseResponse> saveEmployeeContractTermination(
            @RequestBody EmployeeContractEndRequest employeeContractEndRequest,
            HttpServletRequest httpServletRequest
    ) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = employeeService.saveEmployeeContractEnd(token, employeeContractEndRequest);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
