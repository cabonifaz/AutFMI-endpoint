package org.app.autfmi.service.impl;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.LinkTokenDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.TalentRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.PostulantRepository;
import org.app.autfmi.repository.TalentRepository;
import org.app.autfmi.service.IPostulantService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostulantService implements IPostulantService {
    private final PostulantRepository postulantRepository;
    private final TalentRepository talentRepository;
    private final JwtHelper jwt;

    @Override
    public BaseResponse registerPostulant(String token, TalentRequest request) {
        try {
            LinkTokenDTO tokenData = jwt.decodeLinkToken(token);
            BaseRequest baseRequest = Common.createBaseRequest(tokenData.getUserData(), Constante.INSERTAR_TALENTO);

            BaseResponse baseResponse = talentRepository.saveTalent(request, baseRequest);
            if (baseResponse.getIdTipoMensaje() == 2) {
                return postulantRepository.registerPostulant(baseRequest, request.getIdTalento(), tokenData.getLstrRequerimientos());
            } else {
                return baseResponse;
            }
        } catch (Exception e) {
            return new BaseResponse(3, "Error en Service RegisterPostulant::: " + e.getMessage());
        }
    }
}
