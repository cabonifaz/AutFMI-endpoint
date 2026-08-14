package org.app.autfmi.model.dto.seleccion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Fila de Rendimiento: entrevistas vs ingresos de un cliente. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectionPerformanceRowDTO {
  private String cliente;
  private int entrevistas;
  private int ingresos;
}
