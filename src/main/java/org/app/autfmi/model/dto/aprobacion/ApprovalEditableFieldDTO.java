package org.app.autfmi.model.dto.aprobacion;

/**
 * Campo del formulario que un gestor puede editar.
 * tipo: TEXTO | NUMERO | FECHA (yyyy-MM-dd) | SI_NO (0/1).
 */
public record ApprovalEditableFieldDTO(String campo, String etiqueta, String tipo) {
}
