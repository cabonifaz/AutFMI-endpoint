package org.app.autfmi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.response.ParametrosListResponse;
import org.app.autfmi.service.impl.ParametrosService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("fmi/params")
@RequiredArgsConstructor
@Tag(name = "Parametros")
public class ParametrosController {
    private final ParametrosService parametrosService;

    @GetMapping("/list")
    public ResponseEntity<Object> listParametros (@RequestParam String groupIdMaestros) {
        ParametrosListResponse response = parametrosService.listParametros(groupIdMaestros);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
