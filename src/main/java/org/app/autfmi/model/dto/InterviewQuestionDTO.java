package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una pregunta de la entrevista y su respuesta.
 *
 * La pregunta es obligatoria; la respuesta no, porque se registra durante o
 * después de la llamada (ver ENTREVISTAS_PREGUNTAS).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewQuestionDTO {

    private Integer idPregunta;
    private Integer idEntrevista;
    private String pregunta;
    private String respuesta;
    private Integer orden;
}
