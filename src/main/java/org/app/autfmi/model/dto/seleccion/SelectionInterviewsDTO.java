package org.app.autfmi.model.dto.seleccion;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detalle de la sección Entrevistas:
 * - {@code total}/{@code serie}: total GENERAL (rango + cliente), para todos.
 * - {@code porUsuario}: desglose de todos los usuarios (solo Admin).
 * - {@code usuarioTotal}/{@code usuarioSerie}: detalle del usuario objetivo
 *   (Gestor: el suyo; Admin: el elegido). Punto 3.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectionInterviewsDTO {
  private int total;
  private List<SelectionSeriePointDTO> serie;
  private List<SelectionLabelCountDTO> porUsuario;
  private int usuarioTotal;
  private List<SelectionSeriePointDTO> usuarioSerie;
}
