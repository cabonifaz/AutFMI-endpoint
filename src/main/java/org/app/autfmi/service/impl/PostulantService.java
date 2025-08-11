package org.app.autfmi.service.impl;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.LinkTokenDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.TalentRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.PostulantRepository;
import org.app.autfmi.service.IPostulantService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostulantService implements IPostulantService {
    private final PostulantRepository postulantRepository;
    private final JwtHelper jwt;

    @Override
    public BaseResponse registerPostulant(String token, TalentRequest request) {
        try {
            LinkTokenDTO tokenData = jwt.decodeLinkToken(token);
            BaseRequest baseRequest = Common.createBaseRequest(tokenData.getUserData(), Constante.INSERTAR_TALENTO);

            return postulantRepository.registerPostulant(baseRequest, request.getIdTalento(), tokenData.getLstrRequerimientos());

        } catch (Exception e) {
            return new BaseResponse(3, "Error en Service RegisterPostulant::: " + e.getMessage());
        }
    }
}
