package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.app.autfmi.model.request.SolicitudSoftwareRequest;
import org.app.autfmi.model.response.BaseResponse;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEquipoReport {
    private BaseResponse baseResponse;
    private String nombreEmpleado;
    private String apellidosEmpleado;
    private String cliente;
    private String area;
    private String puesto;
    private String fechaSolicitud;
    private String fechaEntrega;
    private Integer idTipoEquipo;
    private String procesador;
    private String ram;
    private String hd;
    private String marca;
    private Integer idAnexo;
    private Boolean celular;
    private Boolean internetMovil;
    private String accesorios;
    private List<SolicitudSoftwareRequest> lstSoftware;
    private String correoGestor;
    private String nombreApellidoGestor;
}
