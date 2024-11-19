package org.app.autfmi.util;

import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.BaseRequest;
import java.util.List;

public class Common {
    public static BaseRequest createBaseRequest(UserDTO userDTO, String funcionalidades) {
        BaseRequest baseRequest = new BaseRequest();
        if (userDTO != null) {
            if (userDTO.getRoles() != null && !userDTO.getRoles().isEmpty()) {
                List<String> roles = userDTO.getRoles();
                String tipoRol = roles.get(0);
                baseRequest.setTipoRol(tipoRol);
            }

            baseRequest.setIdUsuario(userDTO.getIdUsuario());
            baseRequest.setIdEmpresa(userDTO.getIdEmpresa());
            baseRequest.setFuncionalidades(funcionalidades);

            return baseRequest;
        }
        return baseRequest;
    }
}
