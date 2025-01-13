package org.app.autfmi.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.app.autfmi.model.dto.FilePDFDTO;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FilePDFResponse {
    @JsonProperty("result")
    private BaseResponse baseResponse;
    private List<FilePDFDTO> lstArchivos;
}
