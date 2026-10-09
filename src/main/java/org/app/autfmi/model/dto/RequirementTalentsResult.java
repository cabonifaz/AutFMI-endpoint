package org.app.autfmi.model.dto;

import java.util.List;

import org.app.autfmi.model.request.BaseRequest;
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

  /* Gestor (PRIORIDAD 1) y suplente (PRIORIDAD 2) del cliente: destinatarios del
   * correo de confirmación (resultset#7 de SP_REQUERIMIENTO_TALENTO_INS). */
  private List<GestorClienteDTO> gestoresCliente;

  /* Solo en SP_REQUERIMIENTO_CONFIRMACION_PROCESAR: */
  // Talentos reclamados (ESTADO_FORMULARIOS 0 -> 2), a cerrar con ..._MARCAR.
  private List<Integer> talentosReclamados;
  // Quien confirmó por correo (gestor o suplente).
  private GestorClienteDTO confirmador;
  // Usuario que finalizó el ingreso: los SP de reporte validan sus permisos.
  private BaseRequest baseRequestFinaliza;

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
