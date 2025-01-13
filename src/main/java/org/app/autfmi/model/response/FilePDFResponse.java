package org.app.autfmi.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FilePDFResponse {
    @JsonProperty("result")
    private BaseResponse baseResponse;
    private String nombreArchivo;
    private String archivoB64;
}
