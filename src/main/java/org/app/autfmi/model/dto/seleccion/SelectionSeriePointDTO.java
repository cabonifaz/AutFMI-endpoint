package org.app.autfmi.model.dto.seleccion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Punto de una serie temporal (periodo yyyy-MM, cantidad). */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectionSeriePointDTO {
  private String periodo;
  private int cantidad;
}
