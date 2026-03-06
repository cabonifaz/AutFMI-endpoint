package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponseDTO {
  private Integer id;
  private String talento;
  private String tituloRq;
  private String cliente;
  private String fechaEntrevista;
  private String estado;
  private Integer idEstado;
  private String etapa;
  private Integer idEtapa;
}
