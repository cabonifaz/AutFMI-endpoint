package org.app.autfmi.service.impl;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.TalentRepository;
import org.app.autfmi.service.ITalentService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TalentService implements ITalentService {

    private final TalentRepository talentRepository;
    private final JwtHelper jwt;

    @Override
    public BaseResponse listTalents(String token) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_TALENTOS);
        return talentRepository.listTalents(baseRequest);
    }

    @Override
    public BaseResponse getTalent(String token, Integer idTalento) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.MOSTRAR_DATOS_TALENTO);
        return talentRepository.getTalentById(idTalento, baseRequest);
    }
}
