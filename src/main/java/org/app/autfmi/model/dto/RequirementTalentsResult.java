package org.app.autfmi.model.dto;

import java.util.List;

import org.app.autfmi.model.response.BaseResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequirementTalentsResult {
  private BaseResponse baseResponse;
  private List<PostulantDTO> postulantes;

  /*
   * En realidad es la persona que realiza la acción, puede ser o no un gestor
   * Pero la clase ya estaba definida así `GestorRqDTO`
   */
  private GestorRqDTO gestorRq;
  private List<String> ccList;

  private List<ReporteIngreso> reportesIngreso;
  private List<ReporteSolicitudEquipo> reportesSolicitudEquipo;

  @Data
  public static class ReporteIngreso {
    private Integer idHistorial;
    private Integer idTalento;
    private Integer idTipoHistorial;
  }

  @Data
  public static class ReporteSolicitudEquipo {
    private Integer idSolicitudEquipo;
    private Integer idTalento;
  }

}
