package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una pregunta de la entrevista telefónica y su respuesta.
 *
 * La pregunta ya no es texto libre: `idPregunta` es el NUM1 del PARAMETROS con
 * ID_MAESTRO = 55 y `pregunta` es su STRING1, que sólo se devuelve al listar
 * (al guardar viaja el id). La respuesta es opcional: se registra durante o
 * después de la llamada (ver ENTREVISTAS_RESPUESTAS).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewQuestionDTO {

    /** PK de ENTREVISTAS_RESPUESTAS. */
    private Integer idRespuesta;
    private Integer idEntrevista;
    /** Pregunta del maestro 55 (NUM1). */
    private Integer idPregunta;
    /** Texto de la pregunta (STRING1 del maestro 55): sólo de lectura. */
    private String pregunta;
    private String respuesta;
}
