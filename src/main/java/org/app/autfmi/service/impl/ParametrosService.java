package org.app.autfmi.service.impl;

import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.response.ParametrosListResponse;
import org.app.autfmi.repository.ParametrosRepository;
import org.app.autfmi.service.IParametrosService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParametrosService implements IParametrosService {
    private final ParametrosRepository parametrosRepository;

    public ParametrosListResponse listParametros(String groupIdMaestros) {
        ParametrosListResponse response = parametrosRepository.listParametros(groupIdMaestros);
        return response;
    }
}
