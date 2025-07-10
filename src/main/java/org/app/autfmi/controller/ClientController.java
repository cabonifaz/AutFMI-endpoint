package org.app.autfmi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.ContactRegisterRequest;
import org.app.autfmi.model.request.ContactUpdateRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.service.impl.ClientService;
import org.app.autfmi.util.JwtHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
@Tag(name = "Cliente")
public class ClientController {
    private final ClientService clientService;

    @GetMapping("/list")
    public ResponseEntity<BaseResponse> listClients(HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = clientService.listClients(token);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/listContacts")
    public ResponseEntity<BaseResponse> listContacts(HttpServletRequest httpServletRequest, @RequestParam Integer idCliente) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = clientService.listClientContacts(token, idCliente);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/saveContact")
    public ResponseEntity<BaseResponse> saveContact(
            HttpServletRequest httpServletRequest,
            @RequestBody ContactRegisterRequest contacto) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = clientService.saveContact(token, contacto);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/updateContact")
    public ResponseEntity<BaseResponse> updateContact(
            HttpServletRequest httpServletRequest,
            @RequestBody ContactUpdateRequest contacto) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = clientService.updateContact(token, contacto);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

}
