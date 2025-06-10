package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEquipoRequest {
    private Integer idTalento;
    private String nombreEmpleado;
    private String apellidoPaternoEmpleado;
    private String apellidoMaternoEmpleado;
    private Integer idCliente;
    private String cliente;
    private Integer idArea;
    private String area;
    private String puesto;
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
    private Boolean celular;
    private Boolean internetMovil;
    private String accesorios;
    private List<SolicitudSoftwareRequest> lstSoftware;
}
