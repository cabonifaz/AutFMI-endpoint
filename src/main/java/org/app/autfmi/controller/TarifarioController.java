package org.app.autfmi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.service.impl.TarifarioService;
import org.app.autfmi.util.JwtHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/tarifario")
@RequiredArgsConstructor
@Tag(name = "Tarifario")
public class TarifarioController {
    private final TarifarioService tarifarioService;

    @GetMapping("/list")
    public ResponseEntity<BaseResponse> getRequirementsList(
            HttpServletRequest httpServletRequest,
            @RequestParam Integer idCliente) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = tarifarioService.listTarifario(token, idCliente);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
