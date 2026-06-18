package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRequest {
  private Integer idTalento;
  private List<Integer> lstIdRequerimientos;
  private String fecha;
  private String hora;
  private Integer estado;
  private Integer etapa;
  private String enlaceEntrevista;
  private String entrevistadores;
  private String perfil;
}