package org.app.autfmi.service;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import org.app.autfmi.model.request.RequirementFileRequest;
import org.app.autfmi.model.request.RequirementRequest;
import org.app.autfmi.model.request.RequirementTalentRequest;
import org.app.autfmi.model.response.BaseResponse;

import java.util.Date;

public interface IRequirementService {

    BaseResponse listRequirements(String token, Integer nPag, Integer cPag, Integer idCliente, String codigoRQ, Date fechaSolicitud, Integer estado);

    BaseResponse getRequirement(String token, Integer idRequerimiento, Boolean showfiles, Boolean showVacantesList);

    BaseResponse saveRequirement(String token, RequirementRequest request) throws SQLServerException;

    BaseResponse updateRequirement(String token, RequirementRequest request);


    BaseResponse saveRequirementTalents(String token, RequirementTalentRequest request) throws SQLServerException;

    BaseResponse getRequirementTalentData(String token, Integer idTalento);

    BaseResponse saveRequirementFile(String token, RequirementFileRequest request) throws SQLServerException;
    BaseResponse removeRequirementFile(String token, Integer idRqFile);

}
