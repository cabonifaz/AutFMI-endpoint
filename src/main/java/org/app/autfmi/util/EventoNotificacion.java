package org.app.autfmi.util;

/**
 * Eventos de negocio que envían correo. Cada uno se activa o desactiva con su
 * fila del maestro 56 (STRING1 = clave, NUM1 = 1 envía / 0 no envía).
 */
public enum EventoNotificacion {
    RQ_CREACION("CORREO_RQ_CREACION"),
    RQ_ACTUALIZACION("CORREO_RQ_ACTUALIZACION"),
    RQ_AGENTE("CORREO_RQ_AGENTE"),
    RQ_TALENTOS_CONFIRMADOS("CORREO_RQ_TALENTOS_CONFIRMADOS"),
    INGRESO("CORREO_INGRESO"),
    MOVIMIENTO("CORREO_MOVIMIENTO"),
    CESE("CORREO_CESE"),
    SOLICITUD_EQUIPO("CORREO_SOLICITUD_EQUIPO"),
    ENTREVISTA("CORREO_ENTREVISTA"),
    PRONTO_INICIO("CORREO_PRONTO_INICIO");

    private final String clave;

    EventoNotificacion(String clave) {
        this.clave = clave;
    }

    /** STRING1 de la fila del maestro 56. */
    public String clave() {
        return clave;
    }
}
