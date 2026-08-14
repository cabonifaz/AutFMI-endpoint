package org.app.autfmi.model.dto.seleccion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Usuario de selección para el buscador del desglose por-usuario (solo Admin). */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectionUserDTO {
  private Integer idUsuario;
  /** USUCRE: clave con la que se filtran las entrevistas. */
  private String usuario;
  private String nombre;
  private String email;
}
