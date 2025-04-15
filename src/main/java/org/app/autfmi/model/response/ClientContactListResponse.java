package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.ClientContactItemDTO;

import java.util.List;

@Getter
@Setter
public class ClientContactListResponse extends BaseResponse{
    private List<ClientContactItemDTO> lstClientContacts;

    public ClientContactListResponse(Integer idTipoMensaje, String mensaje, List<ClientContactItemDTO> lstClientContacts) {
        super(idTipoMensaje, mensaje);
        this.lstClientContacts = lstClientContacts;
    }
}
