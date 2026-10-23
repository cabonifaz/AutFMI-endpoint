package org.app.autfmi.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Página de postulaciones de un talento (SP_TALENTO_POSTULACIONES_LST). */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TalentApplicationsDTO {

    private List<TalentApplicationDTO> postulaciones;
    /** Postulaciones que pasan la búsqueda y el filtro de estado. */
    private Integer totalElementos;
    private Integer totalPaginas;
    /** Filas por página (PARAMETROS maestro 11); null = sin paginar. */
    private Integer tamanioPagina;
    /** Todas las postulaciones del talento, sin filtros. */
    private Integer totalPostulaciones;
    /** Cantidad por estado del RQ, aplicando sólo la búsqueda. */
    private List<EstadoCantidad> estados;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EstadoCantidad {
        private Integer idEstado;
        private String estado;
        private Integer cantidad;
    }
}
