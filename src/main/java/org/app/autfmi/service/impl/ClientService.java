package org.app.autfmi.service.impl;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.ContactRegisterRequest;
import org.app.autfmi.model.request.ContactUpdateRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.ClientRepository;
import org.app.autfmi.repository.RequirementRepository;
import org.app.autfmi.service.IClientService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientService implements IClientService {
    private final ClientRepository clientRepository;
    private final JwtHelper jwt;

    public BaseResponse listClients(String token){
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_CLIENTES);
        return clientRepository.listClients(baseRequest);
    }

    @Override
    public BaseResponse listClientContacts(String token, Integer idCliente) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_CLIENTES);
        return clientRepository.listClientContacts(baseRequest, idCliente);
    }

    @Override
    public BaseResponse saveContact(String token, ContactRegisterRequest contacto) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.ACTUALIZAR_REQUERIMIENTO);
        return clientRepository.saveContact(baseRequest, contacto);
    }

    @Override
    public BaseResponse updateContact(String token, ContactUpdateRequest contacto) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.ACTUALIZAR_REQUERIMIENTO);
        return clientRepository.updateContact(baseRequest, contacto);
    }

}
