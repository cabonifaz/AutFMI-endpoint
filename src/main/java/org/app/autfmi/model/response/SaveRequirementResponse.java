package org.app.autfmi.model.response;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de creación de requerimiento. Extiende {@link BaseResponse} para
 * conservar {@code idTipoMensaje}/{@code mensaje} planos (compatibles con el
 * front actual) y añade el id del RQ creado y las URLs PUT pre-firmadas de sus
 * archivos, que el front consumirá subiendo directamente a S3.
 */
@Getter
@Setter
@NoArgsConstructor
public class SaveRequirementResponse extends BaseResponse {
    private Integer idRequerimiento;
    private List<RqFileUploadUrlDTO> archivos;
}
