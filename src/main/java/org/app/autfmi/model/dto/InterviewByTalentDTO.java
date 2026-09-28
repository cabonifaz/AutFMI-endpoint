package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entrevista de un talento, en su versión mínima: lo justo para saber si ya
 * tuvo una de cierto tipo y poder abrir la última.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewByTalentDTO {

    private Integer idEntrevista;
    private Integer idTipoEntrevista;
    /** dd/MM/yyyy */
    private String fecha;
    /** HH:mm */
    private String hora;
}
