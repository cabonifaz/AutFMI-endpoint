package org.app.autfmi.model.report;

import org.app.autfmi.model.response.BaseResponse;

public interface IReport {
    BaseResponse getResponse();
    void setResponse(BaseResponse response);
}
