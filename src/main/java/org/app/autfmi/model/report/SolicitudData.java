package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudData {
    public String nombres;
    public String apellidos;
    public String area;
    public String fechaSolicitud;
    public String nombresCreacion;
    public String apellidosCreacion;
    public String nombreUsuarioCreacion;
    public String correoCreacion;
    public String areaCreacion;
    public String usuarioActualModificacion;
    public String usuarioNuevoModificacion;
    public String correoActualModificacion;
    public String correoNuevoModificacion;
    public String nombresCese;
    public String apellidosCese;
    public String usuarioCese;
    public String correoCese;
    public String motivoCese;
    public String firmante;
}
