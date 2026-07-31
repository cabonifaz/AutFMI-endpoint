package org.app.autfmi.model.response;

import java.util.List;

import org.app.autfmi.model.dto.EntrevistadorDTO;
import org.app.autfmi.model.dto.InterviewFileDTO;
import org.app.autfmi.model.dto.InterviewRqDTO;
import org.app.autfmi.model.dto.GrabacionDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDetailResponseDTO {
  private Integer id;
  private Integer idTalento;
  private String talento;
  private String fecha;
  private String hora;
  private Integer idEstado;
  private String estado;
  private Integer idEtapa;
  private String etapa;
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
  private String motivoCancelacion;
  private String perfil;
  private String direccion;
  private String ubicacion;
  private Integer idTipoEntrevista;

  private String clienteResumen;
  private List<InterviewRqDTO> selectedRQs;
  private List<InterviewFileDTO> files;
  private List<EntrevistadorDTO> entrevistadores;
  private List<GrabacionDTO> grabaciones;

}