package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos de contacto del talento que van en la tabla de los correos de
 * solicitud de creación de usuario y de equipo.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContactoTalentoDTO {
    private String nombres;
    private String apellidos;
    private String correo;
    private String celular;

    public static ContactoTalentoDTO from(PostulantDTO postulante) {
        return new ContactoTalentoDTO(postulante.getNombres(), postulante.getApellidos(),
                postulante.getEmail(), postulante.getCelular());
    }
}
