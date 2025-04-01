package org.app.autfmi.service;

import org.app.autfmi.model.request.TalentRequest;
import org.app.autfmi.model.response.BaseResponse;

public interface ITalentService {
    BaseResponse listTalents(String token, Integer nPag, String busqueda);

    BaseResponse getTalent(String token, Integer idTalento);

    BaseResponse saveTalent(String token, TalentRequest talent);

    BaseResponse getTalentsToRequirementList(String token, Integer nPag, String busqueda);
}
