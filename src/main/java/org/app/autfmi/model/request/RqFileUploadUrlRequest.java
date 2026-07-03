package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición para obtener una URL PUT pre-firmada al agregar un archivo a un
 * requerimiento existente (flujo de detalle/actualización).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RqFileUploadUrlRequest {
    private Integer idRequerimiento;
    private Integer idTipoArchivoRQ;
    private String fileName;
    private String contentType;
}
