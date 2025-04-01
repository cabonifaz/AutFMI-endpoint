package org.app.autfmi.service;

import org.app.autfmi.model.response.BaseResponse;

public interface IAuthService {
    BaseResponse login(String username, String password);
}
