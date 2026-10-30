package org.app.autfmi.model.request;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Campos editados del formulario de una solicitud (FTBO-1124). */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalFormUpdateRequest {
  /** Versión leída en el detalle. */
  private Long version;
  /** Campo -> nuevo valor. Solo se aceptan los campos editables del tipo. */
  private Map<String, Object> cambios;
}
