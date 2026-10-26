package org.app.autfmi.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Opciones de un filtro de Requerimientos que se busca en el servidor
 * (contactos, perfiles, postulantes): los SP *_FILTRO_LST comparten forma.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FilterOptionsDTO {

    private List<Opcion> opciones;
    /** Opciones que coinciden con la búsqueda, antes del límite. */
    private Integer total;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Opcion {
        private Integer id;
        private String nombre;
        /** Cargo del contacto o documento del talento; vacío en perfiles. */
        private String detalle;
        /** RQ vigentes relacionados con la opción. */
        private Integer cantidad;
    }
}
