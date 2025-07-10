package org.app.autfmi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.AuthRequest;
import org.app.autfmi.model.response.AuthResponse;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.service.IAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación")
public class AuthController {
    private final IAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse> login(@RequestBody AuthRequest request) {
        try {
            BaseResponse response = authService.login(request.getUsername(), request.getPassword());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
