package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Confirmación de formularios recibida por correo. La envía n8n cuando la IA
 * clasifica la respuesta del gestor/suplente como confirmación. Tipo, ID y
 * código salen del asunto del correo ({@code [TIPO: ... | ID: ... | CODIGO: ...]});
 * el backend vuelve a validar todo.
 *
 * <ul>
 * <li>tipo RQ: id = ID_REQUERIMIENTO, codigo = CODIGO_RQ.</li>
 * <li>tipo INGRESO / MOVIMIENTO / CESE: id = ID_HISTORIAL, codigo = IN-/MV-/CE-&lt;id&gt;.</li>
 * <li>tipo EQUIPO: id = ID_EQUIPO_SOLICITUD, codigo = EQ-&lt;id&gt;.</li>
 * </ul>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailConfirmationRequest {
    private String tipo;
    private Integer id;
    private String codigo;
    private String emailRemitente;
}
