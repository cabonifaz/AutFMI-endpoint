package org.app.autfmi.model.dto;

import java.util.List;

import org.app.autfmi.util.Constante;

/**
 * Origen de los formularios que requieren confirmación del gestor/suplente del
 * cliente. Su nombre viaja en el asunto del correo de confirmación
 * ({@code [TIPO: ... | ID: ... | CODIGO: ...]}) y en el body que envía n8n.
 */
public enum TipoFormulario {

    /** Asignar Talento (Finalizar). ID = ID_REQUERIMIENTO, CODIGO = CODIGO_RQ. */
    RQ(null, null, Constante.GUARDAR_REQUERIMIENTO, "ingreso",
            List.of("Formulario de ingreso", "Solicitud de creación de usuario", "Solicitud de equipo")),

    /** Pantalla Ingreso de FMI. ID = ID_HISTORIAL. */
    INGRESO("IN", Constante.TIPO_REPORTE_INGRESO,
            String.join(",", Constante.INSERTAR_TALENTO, Constante.REALIZAR_INGRESO), "ingreso",
            List.of("Formulario de ingreso", "Solicitud de creación de usuario")),

    /** Pantalla Movimiento de FMI. ID = ID_HISTORIAL. Confirma el cliente de destino. */
    MOVIMIENTO("MV", Constante.TIPO_REPORTE_MOVIMIENTO, Constante.REALIZAR_MOVIMIENTO, "movimiento",
            List.of("Formulario de movimiento")),

    /** Pantalla Cese de FMI. ID = ID_HISTORIAL. */
    CESE("CE", Constante.TIPO_REPORTE_CESE, Constante.REALIZAR_CESE, "cese",
            List.of("Formulario de cese", "Solicitud de desactivación de usuario")),

    /** Pantalla Solicitud de equipo de FMI. ID = ID_EQUIPO_SOLICITUD. */
    EQUIPO("EQ", null, Constante.REALIZAR_MOVIMIENTO, "solicitud de equipo",
            List.of("Solicitud de equipo"));

    private final String prefijoCodigo;
    private final Integer idTipoHistorial;
    private final String funcionalidades;
    private final String etiqueta;
    private final List<String> formularios;

    TipoFormulario(String prefijoCodigo, Integer idTipoHistorial, String funcionalidades, String etiqueta,
            List<String> formularios) {
        this.prefijoCodigo = prefijoCodigo;
        this.idTipoHistorial = idTipoHistorial;
        this.funcionalidades = funcionalidades;
        this.etiqueta = etiqueta;
        this.formularios = formularios;
    }

    /** Código del asunto para los formularios de FMI (el RQ usa su CODIGO_RQ). */
    public String codigo(Integer id) {
        return prefijoCodigo + "-" + id;
    }

    /** Tipo de HISTORIAL (1 ingreso, 2 movimiento, 3 cese); null en RQ y EQUIPO. */
    public Integer getIdTipoHistorial() {
        return idTipoHistorial;
    }

    /** Funcionalidades con las que se regenera el reporte (mismas que al generarlo). */
    public String getFuncionalidades() {
        return funcionalidades;
    }

    /** Texto para el asunto y el correo: "ingreso", "movimiento", ... */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Formularios que se envían al confirmar. */
    public List<String> getFormularios() {
        return formularios;
    }

    /** null si el texto no corresponde a ningún tipo. */
    public static TipoFormulario from(String value) {
        if (value == null) {
            return null;
        }
        for (TipoFormulario t : values()) {
            if (t.name().equalsIgnoreCase(value.trim())) {
                return t;
            }
        }
        return null;
    }
}
