package org.app.autfmi.service.impl;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.TarifarioRepository;
import org.app.autfmi.service.ITarifarioService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TarifarioService implements ITarifarioService {
    private final TarifarioRepository tarifarioRepository;
    private final JwtHelper jwt;

    @Override
    public BaseResponse listTarifario(String token, Integer idCliente) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_TARIFARIO);
        return tarifarioRepository.listaTarifario(baseRequest, idCliente);
    }

}
