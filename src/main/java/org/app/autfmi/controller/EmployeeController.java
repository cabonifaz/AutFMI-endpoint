package org.app.autfmi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.app.autfmi.model.request.SolicitudEquipoRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.FilePDFResponse;
import org.app.autfmi.service.impl.EmployeeService;
import org.app.autfmi.util.JwtHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
@Tag(name = "Empleado")
public class EmployeeController {
    private final EmployeeService employeeService;

    @GetMapping("/data")
    public ResponseEntity<BaseResponse> getEmployee(@RequestParam Integer idTalento) {
        try {
            BaseResponse response = employeeService.getEmployee(idTalento);

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
            @RequestParam Integer idTalento,
            HttpServletRequest httpServletRequest
    ) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            FilePDFResponse response = employeeService.getLastHistory(token, idTipoHistorial, idTalento);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e) {
            return new ResponseEntity<>(
                    new FilePDFResponse(new BaseResponse(3, e.getMessage()), Collections.emptyList()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("solicitud/equipo")
    public ResponseEntity<BaseResponse> solicitudEquipo(
            @RequestBody SolicitudEquipoRequest solicitudEquipoRequest,
            HttpServletRequest httpServletRequest
    ) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = employeeService.solicitudEquipo(token, solicitudEquipoRequest);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/lastSolicitudEquipo")
    public ResponseEntity<FilePDFResponse> getLastSolicitudEquipo(
            HttpServletRequest httpServletRequest,
            @RequestParam Integer idSolicitudEquipo
    ) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            FilePDFResponse response = employeeService.getLastSolicitudEquipo(token, idSolicitudEquipo);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e) {
            return new ResponseEntity<>(
                    new FilePDFResponse(new BaseResponse(3, e.getMessage()), Collections.emptyList()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
