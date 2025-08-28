package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.app.autfmi.model.request.SolicitudSoftwareRequest;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEquipoDTO {
    private String fechaSolicitud;
    private String fechaEntrega;
    private Integer idTipoEquipo;
    private String tipoEquipo;
    private String procesador;
    private String ram;
    private String hd;
    private String marca;
    private String anexo;
    private Integer idAnexo;
    private Boolean bitCelular;
    private Boolean bitInternetMovil;
    private String accesorios;
    private List<SolicitudSoftwareRequest> lstSoftware;
}
