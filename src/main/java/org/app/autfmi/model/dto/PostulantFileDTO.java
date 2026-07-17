package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Archivo de un postulante (REQUERIMIENTO_TALENTO). Representa una fila devuelta
 * por SP_BT_REQUERIMIENTO_TALENTO_ARCHIVO_LST.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostulantFileDTO {
    private Integer idRequerimientoTalentoArchivo;
    private Integer idRequerimiento;
    private Integer idRequerimientoTalento;
    private String nombreArchivo;
    private Integer idTipoArchivo;
    private String rutaArchivo;
}
