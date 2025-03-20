package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Integer idUsuario;
    private Integer idEmpresa;
    private String usuario;
    private String nombres;
    private String apellidos;
    private List<Integer> idRoles;
    private List<String> roles;
}
