package org.app.autfmi.service;

import org.app.autfmi.model.response.BaseResponse;

public interface IClientService {
    BaseResponse listClients (String token);
}
