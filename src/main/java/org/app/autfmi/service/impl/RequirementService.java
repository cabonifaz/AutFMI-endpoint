package org.app.autfmi.service.impl;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.RequirementRequest;
import org.app.autfmi.model.request.RequirementTalentRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.RequirementRepository;
import org.app.autfmi.service.IRequirementService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class RequirementService implements IRequirementService {

    private final RequirementRepository requirementRepository;
    private final JwtHelper jwt;

    @Override
    public BaseResponse listRequirements(String token, Integer nPag, Integer cPag, String cliente, String codigoRQ, Date fechaSolicitud, Integer estado) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_REQUERIMIENTOS);
        return requirementRepository.listRequirements(baseRequest, nPag, cPag, cliente, codigoRQ, fechaSolicitud, estado);
    }

    @Override
    public BaseResponse getRequirement(String token, Integer idRequerimiento, Boolean showfiles) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.DETALLE_REQUERIMIENTO);
        return requirementRepository.getRequirementById(idRequerimiento, showfiles, baseRequest);
    }

    @Override
    public BaseResponse saveRequirement(String token, RequirementRequest request) throws SQLServerException {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = String.join(",", Constante.GUARDAR_REQUERIMIENTO, Constante.ACTUALIZAR_REQUERIMIENTO);
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.saveRequirement(request,  baseRequest);
    }

    @Override
    public BaseResponse saveRequirementTalents(String token, RequirementTalentRequest request) throws SQLServerException {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = String.join(",", Constante.GUARDAR_REQUERIMIENTO, Constante.ACTUALIZAR_REQUERIMIENTO);
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.saveRequirementTalents(request,  baseRequest);
    }
}
