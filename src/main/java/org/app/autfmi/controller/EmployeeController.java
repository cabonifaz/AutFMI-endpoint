package org.app.autfmi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    @GetMapping("/data")
    public ResponseEntity<BaseResponse> getEmployee(@RequestParam Integer idTalento) {
        try {
            BaseResponse response = employeeService.getEmployee(idTalento);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            this.logger.error("Error in getEmployee: ", e);
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<BaseResponse> getEmployeeList(
            @RequestParam @Nullable Integer nPag,
            @RequestParam @Nullable String busqueda,
            HttpServletRequest httpServletRequest) {

        try {
            String authToken = JwtHelper.extractToken(httpServletRequest);
            BaseResponse rs = this.employeeService.findAllEmployees(authToken, nPag, busqueda);
            return new ResponseEntity<>(rs, HttpStatus.OK);
        } catch (Exception e) {
            this.logger.error("Error getting employees: {}", e);
            return new ResponseEntity<>(new BaseResponse(3, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("/entry")
    public ResponseEntity<BaseResponse> saveEmployeeEntry(
            @RequestBody EmployeeEntryRequest employeeEntryRequest,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = employeeService.saveEmployeeEntry(token, employeeEntryRequest);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            this.logger.error("Error in saveEmployeeEntry: ", e);
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/movement")
    public ResponseEntity<BaseResponse> saveEmployeeMovement(
            @RequestBody EmployeeMovementRequest employeeMovementRequest,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = employeeService.saveEmployeeMovement(token, employeeMovementRequest);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            this.logger.error("Error in saveEmployeeMovement: ", e);
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/contractTermination")
    public ResponseEntity<BaseResponse> saveEmployeeContractTermination(
            @RequestBody EmployeeContractEndRequest employeeContractEndRequest,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = employeeService.saveEmployeeContractEnd(token, employeeContractEndRequest);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            this.logger.error("Error in saveEmployeeContractTermination: ", e);
            e.printStackTrace();
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    /** Deshacer el último cese de un talento (reactiva su contrato). */
    @PutMapping("/cese/undo")
    public ResponseEntity<BaseResponse> undoCese(
            @RequestParam Integer idHistorial,
            @RequestParam Integer idTalento,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            return new ResponseEntity<>(employeeService.undoCese(token, idHistorial, idTalento), HttpStatus.OK);
        } catch (Exception e) {
            this.logger.error("Error in undoCese: ", e);
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Deshacer (baja lógica) una solicitud de equipo. */
    @DeleteMapping("/solicitud/equipo")
    public ResponseEntity<BaseResponse> deleteEquipmentRequest(
            @RequestParam Integer idSolicitud,
            @RequestParam Integer idTalento,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            return new ResponseEntity<>(employeeService.deleteEquipmentRequest(token, idSolicitud, idTalento),
                    HttpStatus.OK);
        } catch (Exception e) {
            this.logger.error("Error in deleteEquipmentRequest: ", e);
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/lastHistory")
    public ResponseEntity<FilePDFResponse> getLastHistory(
            @RequestParam Integer idTipoHistorial,
            @RequestParam Integer idTalento,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            FilePDFResponse response = employeeService.getLastHistory(token, idTipoHistorial, idTalento);

            this.logger.info("Response generated for getLastHistory");
            this.logger.info("Response details: {}", response.getBaseResponse().toString());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            this.logger.error("Error in getLastHistory: ", e);
            return new ResponseEntity<>(
                    new FilePDFResponse(new BaseResponse(3, e.getMessage()), Collections.emptyList()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getHistory")
    public ResponseEntity<FilePDFResponse> getHistory(
            @RequestParam Integer historyType,
            @RequestParam Integer movementId,
            @RequestParam Integer talentId,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            /*
             * FilePDFResponse response = employeeService.getLastHistory(token,
             * idTipoHistorial, idTalento);
             */
            this.logger.info("Mov ID: {} His type: {}", movementId, historyType);

            var bs = employeeService.getHistory(token, historyType, movementId, talentId);
            return new ResponseEntity<>(bs, HttpStatus.OK);
        } catch (Exception e) {
            this.logger.error("Error in getLastHistory: ", e);
            var bs = new BaseResponse(3, "Received");
            var response = new FilePDFResponse(bs, Collections.EMPTY_LIST);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("solicitud/equipo")
    public ResponseEntity<BaseResponse> solicitudEquipo(
            @RequestBody SolicitudEquipoRequest solicitudEquipoRequest,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = employeeService.solicitudEquipo(token, solicitudEquipoRequest);
            this.logger.info("Response generated for solicitudEquipo");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            this.logger.error("Error in solicitudEquipo: ", e);
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/lastSolicitudEquipo")
    public ResponseEntity<FilePDFResponse> getLastSolicitudEquipo(
            HttpServletRequest httpServletRequest,
            @RequestParam Integer idTalento) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            FilePDFResponse response = employeeService.getLastSolicitudEquipo(token, idTalento);
            this.logger.info("Response generated for getLastSolicitudEquipo");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            this.logger.error("Error in getLastSolicitudEquipo: ", e);
            return new ResponseEntity<>(
                    new FilePDFResponse(new BaseResponse(3, e.getMessage()), Collections.emptyList()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getRequestedEquipment")
    public ResponseEntity<FilePDFResponse> getRequestEquipement(
            HttpServletRequest httpServletRequest,
            @RequestParam Integer idSolicitud,
            @RequestParam Integer idTalento) {

        try {
            String token = JwtHelper.extractToken(httpServletRequest);

            FilePDFResponse response = employeeService.getRequestEquipement(token,
                    idSolicitud, idTalento);
            this.logger.info("Response generated for getRequestedEquipment");

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            this.logger.error("Error in getRequestedEquipment: ", e);
            return new ResponseEntity<>(
                    new FilePDFResponse(new BaseResponse(3, e.getMessage()), Collections.emptyList()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/detail")
    public ResponseEntity<BaseResponse> getEmployeeFullHistory(
            @RequestParam Integer talentId,
            HttpServletRequest httpServletRequest) {
        try {
            String authToken = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = employeeService.getEmployeeFullHistory(authToken, talentId);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            this.logger.error("Error getting employee full history: {}", e);
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
