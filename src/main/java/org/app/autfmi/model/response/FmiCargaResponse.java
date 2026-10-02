package org.app.autfmi.model.response;

import lombok.Getter;
import lombok.Setter;

/** Resultado de registrar un colaborador desde su FMI. */
@Getter
@Setter
public class FmiCargaResponse extends BaseResponse {

    /** Contrato creado, si se pidió registrarlo. */
    private Integer idContrato;
    /** Movimiento de ingreso creado junto al contrato. */
    private Integer idHistorial;

    public FmiCargaResponse(Integer idTipoMensaje, String mensaje, Integer idContrato,
            Integer idHistorial) {
        super(idTipoMensaje, mensaje);
        this.idContrato = idContrato;
        this.idHistorial = idHistorial;
    }
}
