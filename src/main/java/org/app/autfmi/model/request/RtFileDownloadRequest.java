package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición para obtener una URL GET pre-firmada de descarga de un archivo de
 * postulante (REQUERIMIENTO_TALENTO). La ruta se resuelve en el backend a partir
 * del listado del postulante (no se confía en una ruta enviada por el cliente).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RtFileDownloadRequest {
    private Integer idRequerimientoTalento;
    private Integer idArchivo;
}
