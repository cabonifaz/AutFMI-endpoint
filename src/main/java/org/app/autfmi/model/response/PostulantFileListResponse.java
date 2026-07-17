package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;
import org.app.autfmi.model.dto.PostulantFileDTO;

import java.util.List;

/**
 * Respuesta del listado de archivos de un postulante (REQUERIMIENTO_TALENTO).
 */
@Getter
@Setter
public class PostulantFileListResponse extends BaseResponse {
    private List<PostulantFileDTO> archivos;

    public PostulantFileListResponse(Integer idTipoMensaje, String mensaje, List<PostulantFileDTO> archivos) {
        super(idTipoMensaje, mensaje);
        this.archivos = archivos;
    }
}
