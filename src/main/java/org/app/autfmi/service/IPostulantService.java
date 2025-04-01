package org.app.autfmi.service;

import org.app.autfmi.model.request.TalentRequest;
import org.app.autfmi.model.response.BaseResponse;

public interface IPostulantService {
    BaseResponse registerPostulant(String token, TalentRequest request);
}
