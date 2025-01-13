package org.app.autfmi.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.FilePDFResponse;
import org.app.autfmi.service.impl.EmployeeService;
import org.app.autfmi.util.JwtHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("fmi/employee")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @GetMapping("/data")
    public ResponseEntity<BaseResponse> getEmployee(@RequestParam Integer idUsuarioTalento) {
        try {
            BaseResponse response = employeeService.getEmployee(idUsuarioTalento);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

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
            e.printStackTrace();
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );

        }
    }

    @GetMapping("/lastHistory")
    public ResponseEntity<FilePDFResponse> getLastHistory(
            @RequestParam Integer idTipoHistorial,
            @RequestParam Integer idUsuarioTalento,
            HttpServletRequest httpServletRequest
    ) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            FilePDFResponse response = employeeService.getLastHistory(token, idTipoHistorial, idUsuarioTalento);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e) {
            return new ResponseEntity<>(
                    null,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );

        }
    }
}
