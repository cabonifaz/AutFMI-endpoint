package org.app.autfmi.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * URL PUT pre-firmada devuelta al crear un requerimiento, una por cada archivo.
 * El front sube (PUT) cada archivo en memoria a {@code url}. El orden de la
 * lista coincide con el de {@code lstArchivos} enviado en la creación.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RqFileUploadUrlDTO {
    private String url;
    private String path;
    private String fileName;
    private Integer idTipoArchivoRQ;
}
