package org.app.autfmi.service;

import com.microsoft.sqlserver.jdbc.SQLServerException;

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

import java.util.Date;
import java.util.List;

public interface IRequirementService {

        BaseResponse listRequirements(String token, Integer nPag, Integer cPag, Integer idCliente, String buscar,
                        Date fechaSolicitud, Integer estado);

        BaseResponse getRequirement(String token, Integer idRequerimiento, Boolean showfiles, Boolean showVacantesList,
                        Boolean showContactList);

        BaseResponse saveRequirement(String token, RequirementRequest request) throws SQLServerException;

        BaseResponse updateRequirement(String token, RequirementRequest request) throws SQLServerException;

        BaseResponse saveRequirementTalents(String token, RequirementTalentRequest request) throws SQLServerException;

        BaseResponse getRequirementTalentData(String token, Integer idTalento, Integer idRequerimiento);

        BaseResponse saveRequirementFile(String token, RequirementFileRequest request) throws SQLServerException;

        BaseResponse removeRequirementFile(String token, Integer idRqFile);

        FileResponse getRequirementFile(String token, Integer idrqFile);

        RqPresignedUrlResponse generateRqUploadUrl(String token, RqFileUploadUrlRequest request);

        BaseResponse confirmRqUpload(String token, RqFileConfirmRequest request) throws SQLServerException;

        RqPresignedUrlResponse generateRqDownloadUrl(String token, RqFileDownloadRequest request);

        BaseResponse getTechSkillsForVac(String token, Integer idVacante);

        BaseResponse updateSkillsForVac(String token, Integer idVacante,
                        List<VacanteSkillDTO> skills);

        BaseResponse updateCareersForVac(String token, Integer idVacante, List<VacanteCarreraDTO> careers);

        BaseResponse getCareersForVac(String token, Integer idVacante);

        BaseResponse saveRequirementByAgent(String token, AgentRQRequest request) throws SQLServerException;
}
