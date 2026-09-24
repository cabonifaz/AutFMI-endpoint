package org.app.autfmi.model.request;

import lombok.Data;

/** Edición de una pregunta: normalmente para llenar la respuesta. */
@Data
public class InterviewQuestionUpdateRequest {

    private Integer idPregunta;
    private String pregunta;
    private String respuesta;
}
