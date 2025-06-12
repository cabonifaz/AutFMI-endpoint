package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.app.autfmi.model.response.BaseResponse;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CeseReport implements IReport {
    private BaseResponse response;
    private String nombres;
    private String apellidos;
    private String unidad;
    private String motivo;
    private String fechaHistorial;
    private String correoGestor;
    private String firmante;
    private String firma;
    private String usernameEmpleado;
    private String emailEmpleado;
}
