package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Gestor del cliente (CLIENTE_GESTOR). PRIORIDAD 1 = gestor, 2 = suplente.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GestorClienteDTO {
    private Integer prioridad;
    private Integer idUsuario;
    private String nombres;
    private String apellidos;
    private String correo;
}
