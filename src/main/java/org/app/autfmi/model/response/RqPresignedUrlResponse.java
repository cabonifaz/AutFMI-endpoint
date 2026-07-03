package org.app.autfmi.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de operaciones con URL pre-firmada (subida / descarga) de archivos
 * de requerimiento. En subida lleva {@code url}, {@code path} y {@code fileName};
 * en descarga lleva {@code url} y {@code fileName} ({@code path} nulo).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RqPresignedUrlResponse {
    @JsonProperty("result")
    private BaseResponse baseResponse;
    private String url;
    private String path;
    private String fileName;
}
