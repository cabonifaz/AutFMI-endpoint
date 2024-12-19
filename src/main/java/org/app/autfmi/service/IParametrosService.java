package org.app.autfmi.service;

import org.app.autfmi.model.response.ParametrosListResponse;

public interface IParametrosService {
    ParametrosListResponse listParametros (String groupIdMaestros);
}
