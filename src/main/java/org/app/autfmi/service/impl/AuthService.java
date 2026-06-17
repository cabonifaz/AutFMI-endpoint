package org.app.autfmi.service.impl;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.response.AuthResponse;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.AuthRepository;
import org.app.autfmi.service.IAuthService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final AuthRepository authRepository;

    @Override
    public BaseResponse login(String username, String password) {
        AuthResponse response = authRepository.verifyCredentials(username, password);

        if (response.getIdTipoMensaje() == 1) {
            return response;
        }

        return response;
    }
}
