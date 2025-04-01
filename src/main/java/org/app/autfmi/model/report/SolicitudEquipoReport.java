package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.app.autfmi.model.response.BaseResponse;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudEquipoReport {
    private BaseResponse baseResponse;
    private String correoGestor;
    private String nombres;
    private String apellidos;
}
