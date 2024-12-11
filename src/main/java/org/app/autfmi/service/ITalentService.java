package org.app.autfmi.service;

import org.app.autfmi.model.request.TalentRequest;
import org.app.autfmi.model.response.BaseResponse;

public interface ITalentService {
    BaseResponse listTalents(String token);

    BaseResponse getTalent(String token, Integer idTalento);

    BaseResponse saveTalent(String token, TalentRequest talent);
}
