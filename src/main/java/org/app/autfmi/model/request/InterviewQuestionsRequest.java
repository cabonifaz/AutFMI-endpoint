package org.app.autfmi.model.request;

import java.util.List;

import org.app.autfmi.model.dto.InterviewQuestionDTO;

import lombok.Data;

/**
 * Alta de preguntas de una entrevista: llegan en bloque, como los
 * entrevistadores, porque el formulario las arma como filas.
 */
@Data
public class InterviewQuestionsRequest {

    private Integer idEntrevista;
    private List<InterviewQuestionDTO> preguntas;
}
