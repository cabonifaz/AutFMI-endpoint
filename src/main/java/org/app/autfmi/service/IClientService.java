package org.app.autfmi.service;

import org.app.autfmi.model.request.ContactRegisterRequest;
import org.app.autfmi.model.request.ContactUpdateRequest;
import org.app.autfmi.model.response.BaseResponse;

public interface IClientService {
    BaseResponse listClients (String token);
    BaseResponse listClientContacts(String token, Integer idCliente);

    BaseResponse saveContact (String token, ContactRegisterRequest contacto);
    BaseResponse updateContact(String token, ContactUpdateRequest contacto);
}
