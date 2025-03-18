package org.app.autfmi.controller;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.RequirementRequest;
import org.app.autfmi.model.request.RequirementTalentRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.service.impl.RequirementService;
import org.app.autfmi.util.JwtHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("fmi/requirement")
@RequiredArgsConstructor
public class RequirementController {
    private final RequirementService requirementService;

    @GetMapping("/list")
    public ResponseEntity<BaseResponse> getRequirementsList(
            @RequestParam @Nullable Integer nPag,
            @RequestParam @Nullable Integer cPag,
            @RequestParam @Nullable Integer idCliente,
            @RequestParam @Nullable String codigoRQ,
            @RequestParam @Nullable Date fechaSolicitud,
            @RequestParam @Nullable Integer estado,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.listRequirements(token, nPag, cPag, idCliente, codigoRQ, fechaSolicitud, estado);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/data")
    public ResponseEntity<BaseResponse> getRequirement(
            @RequestParam Integer idRequerimiento,
            @RequestParam Boolean showfiles,
            HttpServletRequest httpServletRequest
    ) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.getRequirement(token, idRequerimiento, showfiles);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/save")
    public ResponseEntity<BaseResponse> saveRequirement(
            @RequestBody RequirementRequest request,
            HttpServletRequest httpServletRequest
    ) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.saveRequirement(token, request);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/update")
    public ResponseEntity<BaseResponse> updateRequirement(
            @RequestBody RequirementRequest request,
            HttpServletRequest httpServletRequest
    ) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.updateRequirement(token, request);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/talents/save")
    public ResponseEntity<BaseResponse> saveRequirementTalents(
            @RequestBody RequirementTalentRequest request,
            HttpServletRequest httpServletRequest
    ) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.saveRequirementTalents(token, request);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }



    @DeleteMapping("/file/remove")
    public ResponseEntity<BaseResponse> removeRequirementFile(
            @RequestParam Integer idRqFile,
            HttpServletRequest httpServletRequest
    ) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.removeRequirementFile(token, idRqFile);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
