package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición para confirmar (registrar en BD) un archivo de postulante
 * (REQUERIMIENTO_TALENTO) ya subido a S3 mediante una URL pre-firmada.
 * El {@code path} es la ruta S3 devuelta por el endpoint upload-url.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RtFileConfirmRequest {
    private Integer idRequerimiento;
    private Integer idRequerimientoTalento;
    private String nombreArchivo;
    private Integer idTipoArchivo;
    private String path;
}
