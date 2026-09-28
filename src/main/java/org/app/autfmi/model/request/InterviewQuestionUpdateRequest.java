package org.app.autfmi.model.request;

import lombok.Data;

/** Edición de una respuesta: normalmente para llenar o corregir el texto. */
@Data
public class InterviewQuestionUpdateRequest {

    /** PK de ENTREVISTAS_RESPUESTAS. */
    private Integer idRespuesta;
    /** Pregunta del maestro 55 a la que responde. */
    private Integer idPregunta;
    private String respuesta;
}
