package org.app.autfmi.model.dto.seleccion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** KPIs ligeros de la página Resumen del módulo Selección. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectionSummaryDTO {
  private int totalEntrevistas;
  private int totalIngresos;
}
