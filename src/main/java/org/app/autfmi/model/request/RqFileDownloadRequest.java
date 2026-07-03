package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición para obtener una URL GET pre-firmada de descarga de un archivo de
 * requerimiento.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RqFileDownloadRequest {
    private Integer idArchivo;
}
