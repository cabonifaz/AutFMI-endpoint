package org.app.autfmi.service;

import org.app.autfmi.model.request.RequirementRequest;
import org.app.autfmi.model.response.BaseResponse;

import java.util.Date;

public interface IRequirementService {

    BaseResponse listRequirements(String token, Integer nPag, Integer cPag, String cliente, String codigoRQ, Date fechaSolicitud, Integer estado);

    BaseResponse getRequirement(String token, Integer idRequerimiento, Boolean showfiles);

    BaseResponse saveRequirement(String token, RequirementRequest request);
}
