package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición para obtener una URL PUT pre-firmada al agregar un archivo a un
 * postulante (REQUERIMIENTO_TALENTO) de un requerimiento.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RtFileUploadUrlRequest {
    private Integer idRequerimiento;
    private Integer idRequerimientoTalento;
    private String fileName;
    private String contentType;
}
