package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileRequest {
    private String string64;
    private String nombreArchivo;
    private String extensionArchivo;
    private Integer idTipoArchivo;
    private Integer idTipoArchivoRQ;
    // MIME type usado para firmar la URL PUT (flujo de carga directa a S3).
    // Solo se usa cuando el archivo se sube por URL pre-firmada (no base64).
    private String contentType;
}
