package org.app.autfmi.util;

import java.util.Set;

public class Constante {

    // ID_MAESTROS
    public static final String NOTIFICACION_RQ_EMAILS = "42";
    public static final String TIPO_ENTREVISTA = "47";

    // TIPO DE ENTREVISTA (valores string1 del maestro 47)
    public static final String TIPO_ENTREVISTA_PRESENCIAL = "PRESENCIAL";
    public static final String TIPO_ENTREVISTA_VIRTUAL = "VIRTUAL";

    // TIPO DE ARCHIVO DE ENTREVISTA (maestro 45). El ICS es generado por el sistema.
    public static final int TIPO_ARCHIVO_ENTREVISTA_ICS = 4;

    // REPOSITORIO
    public static final String RUTA_REPOSITORIO = "repositorio/";
    public static final String RUTA_RQ_ARCHIVOS = "/[ID_REQUERIMIENTO]/archivos/";
    public static final String RUTA_RT_ARCHIVOS = "/postulantes/[ID_REQUERIMIENTO_TALENTO]/archivos/";

    // VALIDACIÓN DE ARCHIVOS DE POSTULANTE
    public static final Set<String> EXT_ARCHIVO_POSTULANTE = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg", "webp", "zip");
    public static final long MAX_TAMANIO_ARCHIVO_POSTULANTE = 10L * 1024 * 1024; // 10 MB
    public static final String RUTA_FIRMAS = "/firmas/[ID_USUARIO]/";
    public static final String RUTA_INTERVIEW = "/interviews/[ID_INTERVIEW]/archivos/";

    // TIPO REPORTES

    public static final int TIPO_REPORTE_CESE = 3;
    public static final int TIPO_REPORTE_INGRESO = 1;
    public static final int TIPO_REPORTE_MOVIMIENTO = 2;

    // AREAS (nombre de PARAMETROS maestro 7). Cuando el area del movimiento es
    // Outsourcing, el formulario muestra "Cliente" en lugar de "Equipo".
    public static final String AREA_OUTSOURCING = "Outsourcing";

    // CORREOS DESTINO DE FORMULARIOS (string1 del PARAMETROS del maestro indicado).
    // TO por formulario: Ingreso/Movimiento/Cese -> 52; Creacion de Usuario/Equipo -> 51.
    // Selección (maestro 35) va siempre en copia (CC) junto al usuario generador.
    public static final String MAESTRO_CORREO_SOPORTE = "51";
    public static final String MAESTRO_CORREO_TALENTO = "52";
    public static final String MAESTRO_CORREO_SELECCION = "35";

    // FUNCIONALIDADES
    public static final String LISTAR_TALENTOS = "1";
    public static final String MOSTRAR_DATOS_TALENTO = "2";
    public static final String INSERTAR_TALENTO = "3";
    public static final String ACTUALIZAR_USUARIO = "4";
    public static final String REALIZAR_INGRESO = "5";
    public static final String REALIZAR_MOVIMIENTO = "7";
    public static final String REALIZAR_CESE = "8";
    public static final String OBTENER_ULTIMO_REGISTRO_HISTORIAL = "10";
    public static final String REALIZAR_SOLICITUD_EQUIPO = "14";
    public static final String LISTAR_REQUERIMIENTOS = "16";
    public static final String GUARDAR_REQUERIMIENTO = "18";
    public static final String ACTUALIZAR_REQUERIMIENTO = "19";
    public static final String DETALLE_REQUERIMIENTO = "17";
    public static final String LISTAR_CLIENTES = "20";

    public static final String GUARDAR_ARCHIVOS = "21";
    public static final String ACTUALIZAR_ARCHIVOS = "22";
    public static final String LISTAR_ARCHIVOS = "23";
    public static final String CARGAR_ARCHIVOS = "24";
    public static final String ELIMINAR_ARCHIVOS = "25";

    public static final String LISTAR_TARIFARIO = "26";

    public static final String DOMINIO_CORREO = "@fractalservicios.pe";
    public static final String GUARDAR_INTERVIEW = "27";

    // Interviews
    public static final String CREATE_INTERVIEW = "1030";
    public static final String UPDATE_INTERVIEW = "1031";
    public static final String VIEW_INTERVIEW = "1032";
    public static final String LIST_INTERVIEW = "1033";
    public static final String UPLOAD_DOWNLOAD_INTERVIEW_FILE = "1034";

    // Deshacer el último movimiento (Cese / Solicitud de equipo). Solo SUPERADMIN.
    public static final String DESHACER_MOVIMIENTO = "2048";

    // TRAZABILIDAD
    public static final String TXT_SEPARADOR = "=========================================";

    // CORREO
    public static final String LIST_TALENT_ROW = """
                <tr>
                    <td>{{numFila}}</td>
                    <td>{{nombres}}</td>
                    <td>{{apellidos}}</td>
                    <td>{{docIdentidad}}</td>
                    <td>{{numCelular}}</td>
                    <td>{{correo}}</td>
                    <td>{{fchInicioLabores}}</td>
                    <td>{{tiempoContrato}} (inicialmente)</td>
                    <td>{{cargo}}</td>
                    <td>{{remuneracion}}</td>
                    <td>{{modalidad}}</td>
                    <td>{{tieneEquipo}}</td>
                </tr>
            """;
    public static final String CUERPO_CORREO = """
            <!DOCTYPE html>
            <html lang="es">

            <head>
                <style>
                    table {
                        width: 100%;
                        border-collapse: collapse;
                    }

                    td,
                    th {
                        padding: 5px;
                        text-align: left;
                        text-align: center;
                    }

                    .table-header {
                        background-color: #2D5294;
                        color: #fff;
                        text-align: center;
                    }
                </style>
            </head>

            <body>
                <div>
                    Estimado [GESTOR],
                </div>
                <br>
                <div>
                    Comunicarte que se está gestionando el ingreso de un nuevo talento para el equipo [CLIENTE].
                </div>
                <div>
                    Comparto sus datos para la elaboración del Formulario de [TIPO_FORMULARIO] y la solicitud de creación de usuario.
                </div>
                <br>
                <br>

                <table border="1">
                    <tr>
                        <th class="table-header">Item</th>
                        <th class="table-header">Nombres</th>
                        <th class="table-header">Apellidos</th>
                        <th class="table-header">Doc. Identidad</th>
                        <th class="table-header">Celular</th>
                        <th class="table-header">Correo</th>
                        <th class="table-header">Inicio de Labores</th>
                        <th class="table-header">Tiempo de Contrato</th>
                        <th class="table-header">Cargo</th>
                        <th class="table-header">Remuneración</th>
                        <th class="table-header">Modalidad</th>
                        <th class="table-header">Equipo Propio</th>
                    </tr>
                    {{listaTalentos}}
                </table>

                <br>
                <div>
                    Saludos,
                </div>
                <br>
                <br>
            </body>

            </html>
            """;
}
