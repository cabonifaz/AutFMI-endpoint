package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.ClientItemDTO;

import java.util.List;

@Getter
@Setter
public class ClientListResponse extends BaseResponse {
    private List<ClientItemDTO> clientes;

    public ClientListResponse(Integer idTipoMensaje, String mensaje, List<ClientItemDTO> clientes) {
        super(idTipoMensaje, mensaje);
        this.clientes = clientes;
    }
}
