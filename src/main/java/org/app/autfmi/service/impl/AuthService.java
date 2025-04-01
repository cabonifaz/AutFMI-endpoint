package org.app.autfmi.service.impl;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.response.AuthResponse;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.AuthRepository;
import org.app.autfmi.service.IAuthService;
import org.springframework.stereotype.Service;
import org.app.autfmi.util.JwtHelper;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final AuthRepository authRepository;
    private final JwtHelper jwt;

    @Override
    public BaseResponse login(String username, String password) {
        BaseResponse baseResponse = authRepository.verifyCredentials(username, password);

        if (baseResponse.getIdTipoMensaje() == 1) {
            return baseResponse;
        }

        var userData = authRepository.getUserByUsername(username);
        var userDto = authRepository.mapToUserDTO(userData);
        String token = jwt.generateToken(userDto);

        return new AuthResponse(baseResponse.getIdTipoMensaje(), baseResponse.getMensaje(), token);
    }
}
