package org.app.autfmi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.TalentRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.service.impl.TalentService;
import org.app.autfmi.util.JwtHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/talent")
@RequiredArgsConstructor
@Tag(name = "Talento")
public class TalentController {

    private final TalentService talentService;

    @GetMapping("/list")
    public ResponseEntity<BaseResponse> getTalentsList(
            @RequestParam @Nullable Integer nPag,
            @RequestParam @Nullable String busqueda,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = talentService.listTalents(token,nPag, busqueda);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/data")
    public ResponseEntity<BaseResponse> getTalent(@RequestParam Integer idTalento, HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = talentService.getTalent(token, idTalento);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/save")
    public ResponseEntity<BaseResponse> saveTalent(
            @RequestBody TalentRequest talent,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = talentService.saveTalent(token, talent);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/requirement/list")
    public ResponseEntity<BaseResponse> getTalentsToRequirementList(
            @RequestParam @Nullable Integer nPag,
            @RequestParam @Nullable String busqueda,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = talentService.getTalentsToRequirementList(token,nPag, busqueda);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Candidatos a que una persona ya exista en el banco de talentos, para la
     * carga de colaboradores desde un FMI.
     *
     * Se busca por nombre (lo unico que trae el formulario) y, en la segunda
     * pasada, por DNI o correo, que son las claves que aporta el CV y las
     * unicas que dan certeza. Devuelve DNI, correo y situacion para que el
     * operador pueda distinguir homonimos.
     */
    @GetMapping("/search-identity")
    public ResponseEntity<BaseResponse> searchIdentity(
            @RequestParam @Nullable String busqueda,
            @RequestParam @Nullable String dni,
            @RequestParam @Nullable String email,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = talentService.searchIdentity(token, busqueda, dni, email);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
