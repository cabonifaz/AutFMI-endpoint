package org.app.autfmi.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.service.impl.TalentService;
import org.app.autfmi.util.JwtHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("fmi/talent")
@RequiredArgsConstructor
public class TalentController {

    private final TalentService talentService;

    @GetMapping("/list")
    public ResponseEntity<BaseResponse> listTalents(HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = talentService.listTalents(token);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/data")
    public ResponseEntity<BaseResponse> getTalent(@RequestParam Integer idTalento,HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = talentService.getTalent(token, idTalento);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
