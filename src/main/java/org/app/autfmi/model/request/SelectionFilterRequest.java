package org.app.autfmi.model.request;

import lombok.Data;

/**
 * Filtros de las estadísticas del módulo Selección. {@code idCliente} es opcional
 * (null = todos los clientes). Las fechas van en formato yyyy-MM-dd.
 */
@Data
public class SelectionFilterRequest {
  private String fechaIni;
  private String fechaFin;
  private Integer idCliente;
  /** Usuario de selección (USUCRE) elegido por el Admin. Solo aplica a Entrevistas. */
  private String usucre;
}
