package org.app.autfmi.util;

import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.BaseRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Common {
    public static BaseRequest createBaseRequest(UserDTO userDTO, String funcionalidades) {
        BaseRequest baseRequest = new BaseRequest();
        if (userDTO != null) {
            if (userDTO.getRoles() != null && !userDTO.getRoles().isEmpty()) {
                List<Integer> roles = userDTO.getRoles();
                Integer idRol = roles.get(0);
                baseRequest.setIdRol(idRol);
            }

            baseRequest.setIdUsuario(userDTO.getIdUsuario());
            baseRequest.setIdEmpresa(userDTO.getIdEmpresa());
            baseRequest.setFuncionalidades(funcionalidades);
            baseRequest.setUsername(userDTO.getUsuario());

            return baseRequest;
        }
        return baseRequest;
    }

    public static LocalDate formatDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (date != null && !date.isEmpty()) {
            return LocalDate.parse(date, formatter);
        }
        return null;
    }
}
