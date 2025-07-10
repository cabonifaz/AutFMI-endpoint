package org.app.autfmi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.TalentRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.service.impl.PostulantService;
import org.app.autfmi.util.JwtHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/postulant")
@RequiredArgsConstructor
@Tag(name = "Postulante")
public class PostulantController {
    private final PostulantService postulantService;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse> registerPostulant(
            @RequestBody TalentRequest request,
            HttpServletRequest httpServletRequest
    ) {
        BaseResponse response = new BaseResponse();

        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            response = postulantService.registerPostulant(token, request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.setIdTipoMensaje(3);
            response.setMensaje(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
