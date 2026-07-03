package org.app.autfmi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.app.autfmi.model.dto.VacanteCarreraDTO;
import org.app.autfmi.model.dto.VacanteSkillDTO;
import org.app.autfmi.model.request.AgentRQRequest;
import org.app.autfmi.model.request.RequirementFileRequest;
import org.app.autfmi.model.request.RequirementRequest;
import org.app.autfmi.model.request.RequirementTalentRequest;
import org.app.autfmi.model.request.RqFileConfirmRequest;
import org.app.autfmi.model.request.RqFileDownloadRequest;
import org.app.autfmi.model.request.RqFileUploadUrlRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.FileResponse;
import org.app.autfmi.model.response.RqPresignedUrlResponse;
import org.app.autfmi.model.response.VacanteSkillsResponse;
import org.app.autfmi.service.impl.RequirementService;
import org.app.autfmi.util.JwtHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/requirement")
@RequiredArgsConstructor
@Tag(name = "Requerimiento")
public class RequirementController {
    private final RequirementService requirementService;
    private final Logger logger = LoggerFactory.getLogger(RequirementController.class);

    @GetMapping("/list")
    public ResponseEntity<BaseResponse> getRequirementsList(
            @RequestParam @Nullable Integer nPag,
            @RequestParam @Nullable Integer cPag,
            @RequestParam @Nullable Integer idCliente,
            @RequestParam @Nullable String buscar,
            @RequestParam @Nullable Date fechaSolicitud,
            @RequestParam @Nullable Integer estado,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.listRequirements(token, nPag, cPag, idCliente, buscar,
                    fechaSolicitud, estado);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/data")
    public ResponseEntity<BaseResponse> getRequirement(
            @RequestParam Integer idRequerimiento,
            @RequestParam Boolean showfiles,
            @RequestParam Boolean showVacantesList,
            @RequestParam Boolean showContactList,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.getRequirement(token, idRequerimiento, showfiles,
                    showVacantesList, showContactList);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getStackTrace());
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/save")
    public ResponseEntity<BaseResponse> saveRequirement(
            @RequestBody RequirementRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.saveRequirement(token, request);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/savebyagent")
    public ResponseEntity<BaseResponse> saveRequirementByAgent(
            @RequestBody AgentRQRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.saveRequirementByAgent(token, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            this.logger.error("Error", e.getMessage());
            return new ResponseEntity<>(
                    new BaseResponse(3, "Ha ocurrido un error al guardar el requerimiento", e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/update")
    public ResponseEntity<BaseResponse> updateRequirement(
            @RequestBody RequirementRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.updateRequirement(token, request);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/talents/save")
    public ResponseEntity<BaseResponse> saveRequirementTalents(
            @RequestBody RequirementTalentRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.saveRequirementTalents(token, request);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/talents/data")
    public ResponseEntity<BaseResponse> getRequirementTalentData(
            @RequestParam Integer idTalento,
            @RequestParam Integer idRequerimiento,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.getRequirementTalentData(token, idTalento, idRequerimiento);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/file/save")
    public ResponseEntity<BaseResponse> saveRequirementFile(
            @RequestBody RequirementFileRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.saveRequirementFile(token, request);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/file/remove")
    public ResponseEntity<BaseResponse> removeRequirementFile(
            @RequestParam Integer idRqFile,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.removeRequirementFile(token, idRqFile);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new BaseResponse(3, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/file")
    public ResponseEntity<FileResponse> getRequirementFiles(
            @RequestParam Integer idArchivo,
            HttpServletRequest httpServletRequest) {
        FileResponse fileResponse = new FileResponse();
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            fileResponse = requirementService.getRequirementFile(token, idArchivo);
            return ResponseEntity.ok(fileResponse);
        } catch (Exception e) {
            fileResponse.setBaseResponse(new BaseResponse(3, e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(fileResponse);
        }
    }

    @PostMapping("/file/upload-url")
    public ResponseEntity<RqPresignedUrlResponse> generateUploadUrl(
            @RequestBody RqFileUploadUrlRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            RqPresignedUrlResponse response = requirementService.generateRqUploadUrl(token, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RqPresignedUrlResponse(new BaseResponse(3, e.getMessage()), null, null, null));
        }
    }

    @PostMapping("/file/confirm-upload")
    public ResponseEntity<BaseResponse> confirmUpload(
            @RequestBody RqFileConfirmRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.confirmRqUpload(token, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse(3, e.getMessage()));
        }
    }

    @PostMapping("/file/download-url")
    public ResponseEntity<RqPresignedUrlResponse> generateDownloadUrl(
            @RequestBody RqFileDownloadRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            RqPresignedUrlResponse response = requirementService.generateRqDownloadUrl(token, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RqPresignedUrlResponse(new BaseResponse(3, e.getMessage()), null, null, null));
        }
    }

    @GetMapping("/vacante/techskills")
    public ResponseEntity<VacanteSkillsResponse> getTechSkillsForVac(
            @RequestParam Integer idVacante,
            HttpServletRequest httpServletRequest) {

        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            VacanteSkillsResponse response = requirementService.getTechSkillsForVac(token, idVacante);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            VacanteSkillsResponse errResponse = new VacanteSkillsResponse(
                    3, "Ocurrió un error al obtener las habilidades técnicas de la vacante",
                    null);
            return ResponseEntity.internalServerError().body(errResponse);
        }
    }

    @PostMapping("/vacantes/skills/update")
    public ResponseEntity<BaseResponse> updateSkillsForVac(
            @RequestParam Integer idVacante,
            @RequestBody List<VacanteSkillDTO> request,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.updateSkillsForVac(token, idVacante, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    new BaseResponse(3, e.getMessage()));
        }
    }

    @PostMapping("/vacantes/careers/update")
    public ResponseEntity<BaseResponse> updateCareersForVac(
            @RequestParam Integer idVacante,
            @RequestBody List<VacanteCarreraDTO> request,
            HttpServletRequest httpServletRequest) {

        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.updateCareersForVac(token, idVacante, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            BaseResponse bRes = new BaseResponse(3, "Error al listar las carreras para la vacante");
            return ResponseEntity.internalServerError().body(bRes);
        }
    }

    @GetMapping("/vacantes/careers")
    public ResponseEntity<BaseResponse> getMethodName(@RequestParam Integer idVacante,
            HttpServletRequest httpServletRequest) {
        try {
            String token = JwtHelper.extractToken(httpServletRequest);
            BaseResponse response = requirementService.getCareersForVac(token, idVacante);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            BaseResponse bRes = new BaseResponse(3, "Error al listar las carreras para la vacante");
            return ResponseEntity.internalServerError().body(bRes);
        }
    }

}
