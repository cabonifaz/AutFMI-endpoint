package org.app.autfmi.service.impl;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import lombok.RequiredArgsConstructor;

import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.dto.VacanteCarreraDTO;
import org.app.autfmi.model.dto.VacanteSkillDTO;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.FileResponse;
import org.app.autfmi.model.response.VacanteSkillsResponse;
import org.app.autfmi.repository.RequirementRepository;
import org.app.autfmi.service.IRequirementService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.FileUtils;
import org.app.autfmi.util.JwtHelper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequirementService implements IRequirementService {

    private final RequirementRepository requirementRepository;
    private final JwtHelper jwt;

    @Override
    public BaseResponse listRequirements(String token, Integer nPag, Integer cPag, Integer idCliente, String buscar,
            Date fechaSolicitud, Integer estado) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_REQUERIMIENTOS);
        return requirementRepository.listRequirements(baseRequest, nPag, cPag, idCliente, buscar, fechaSolicitud,
                estado);
    }

    @Override
    public BaseResponse getRequirement(String token, Integer idRequerimiento, Boolean showfiles,
            Boolean showVacantesList, Boolean showContactList) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.DETALLE_REQUERIMIENTO);
        return requirementRepository.getRequirementById(idRequerimiento, showfiles, showVacantesList, showContactList,
                baseRequest);
    }

    @Override
    public BaseResponse saveRequirement(String token, RequirementRequest request) throws SQLServerException {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = Constante.GUARDAR_REQUERIMIENTO;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.saveRequirement(request, baseRequest);
    }

    @Override
    public BaseResponse updateRequirement(String token, RequirementRequest request) throws SQLServerException {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = Constante.ACTUALIZAR_REQUERIMIENTO;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.updateRequirement(request, baseRequest);
    }

    @Override
    public BaseResponse saveRequirementTalents(String token, RequirementTalentRequest request) {
        try {
            UserDTO user = jwt.decodeToken(token);
            String funcionalidades = Constante.GUARDAR_REQUERIMIENTO;
            BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
            return requirementRepository.saveRequirementTalents(request, baseRequest);
        } catch (Exception e) {
            return new BaseResponse(3, e.getMessage());
        }

    }

    @Override
    public BaseResponse getRequirementTalentData(String token, Integer idTalento, Integer idRequerimiento) {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = Constante.MOSTRAR_DATOS_TALENTO;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.getRequirementTalentData(baseRequest, idTalento, idRequerimiento);
    }

    @Override
    public BaseResponse saveRequirementFile(String token, RequirementFileRequest request) throws SQLServerException {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = Constante.GUARDAR_ARCHIVOS;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.saveRequirementFile(baseRequest, request);
    }

    @Override
    public BaseResponse removeRequirementFile(String token, Integer idRqFile) {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = Constante.ELIMINAR_ARCHIVOS;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.removeRequirementFile(baseRequest, idRqFile);
    }

    @Override
    public FileResponse getRequirementFile(String token, Integer idrqFile) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_REQUERIMIENTOS);
        FileResponse fileResponse = requirementRepository.getRqFile(baseRequest, idrqFile);
        String fileBase64 = "";
        if (fileResponse.getBaseResponse().getIdTipoMensaje() == 2 && fileResponse != null) {
            fileBase64 = FileUtils.cargarArchivoAws(fileResponse.getFile());
            fileResponse.setFile(fileBase64);
        }

        if (fileBase64.isEmpty()) {
            fileResponse.setBaseResponse(new BaseResponse(1, "Archivo no encontrado"));
        }

        return fileResponse;
    }

    @Override
    public VacanteSkillsResponse getTechSkillsForVac(String token, Integer idVacante) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = new BaseRequest();
        baseRequest.setUsername(user.getUsuario());
        baseRequest.setIdRol(user.getIdRoles().get(0));
        baseRequest.setIdUsuario(user.getIdUsuario());
        return requirementRepository.getTechSkillsForVac(idVacante);
    }

    @Override
    public BaseResponse updateSkillsForVac(String token, Integer idVacante,
            List<VacanteSkillDTO> skills) {

        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, "");
        return requirementRepository.updateSkillsForVac(baseRequest, idVacante, skills);

    }

    @Override
    public BaseResponse updateCareersForVac(String token, Integer idVacante, List<VacanteCarreraDTO> careers) {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = Constante.ACTUALIZAR_REQUERIMIENTO;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.updateCareersForVac(idVacante, baseRequest, careers);
    }

    @Override
    public BaseResponse getCareersForVac(String token, Integer idVacante) {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = Constante.LISTAR_REQUERIMIENTOS;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.getCareersForVac(baseRequest, idVacante);
    }

}
