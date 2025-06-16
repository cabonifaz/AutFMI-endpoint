package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.RequirementItemDTO;

import java.util.List;

@Getter
@Setter
public class RequirementListResponse extends BaseResponse{
    private List<RequirementItemDTO> requerimientos;
    private Integer totalElementos;
    private Integer totalPaginas;

    public RequirementListResponse(Integer idTipoMensaje, String mensaje, List<RequirementItemDTO> requerimientos, Integer totalElementos, Integer totalPaginas) {
        super(idTipoMensaje, mensaje);
        this.requerimientos = requerimientos;
        this.totalElementos = totalElementos;
        this.totalPaginas = totalPaginas;
    }
}
