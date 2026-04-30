package org.app.autfmi.model.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewUpdateRequest {
  private Integer idEntrevista;
  private Integer idTalento;
  private String fecha;
  private String hora;
  private Integer estado;
  private Integer etapa;
  private String enlaceEntrevista;
  private Integer calificacion;
  private Integer calificacionPersonal;
  private Integer calificacionExperiencia;
  private Integer calificacionIdiomas;
  private Integer calificacionEducacion;
  private String notasPersonales;
  private String notasExperiencia;
  private String notasIdiomas;
  private String notasEducacion;
  private List<Integer> idsRqs;
  private String entrevistadores;
  private String grabaciones;
  private String motivoCancelacion;
}