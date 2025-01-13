package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FilePDFDTO {
    private String nombreArchivo;
    private String archivoB64;
}
