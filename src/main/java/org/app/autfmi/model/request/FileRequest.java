package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileRequest {
    private String string64;
    private String nombreArchivo;
    private String extensionArchivo;
    private Integer idTipoArchivo;
}
