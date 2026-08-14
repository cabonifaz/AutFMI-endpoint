package org.app.autfmi.model.dto.seleccion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Par etiqueta/cantidad genérico. Se usa para "entrevistas por usuario" y para
 * "ingresos por cliente" (en este último {@code id} lleva el ID_CLIENTE si existe).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectionLabelCountDTO {
  private Integer id;
  private String label;
  private int cantidad;
}
