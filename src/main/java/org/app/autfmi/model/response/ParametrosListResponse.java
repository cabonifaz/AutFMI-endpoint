package org.app.autfmi.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.ParametrosDTO;

import java.util.List;

@Getter
@Setter
public class ParametrosListResponse {
    @JsonProperty("result")
    private BaseResponse baseResponse;
    private List<ParametrosDTO> listParametros;
}
