package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una postulación del talento: un requerimiento al que fue asignado, con el
 * estado del RQ (maestro 24) y el del talento dentro de él (maestro 25).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TalentApplicationDTO {

    private Integer idRequerimiento;
    private String codigo;
    private String titulo;
    private String cliente;
    /** dd/MM/yyyy */
    private String fechaSolicitud;
    private Integer idEstado;
    private String estado;
    private Integer idEstadoTalento;
    private String estadoTalento;
    /** El talento ingresó a la empresa por este RQ (INGRESO = 1). */
    private Boolean ingreso;
    /** Vacante a la que postuló: perfil profesional (maestro 14). */
    private Integer idPerfil;
    private String vacante;
    /** Talentos asignados al RQ, incluido éste. */
    private Integer postulantes;
}
