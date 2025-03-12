package org.app.autfmi.util;

public class Constante {
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

    // FORMS
    public static final String LIST_ITEM = """
            <tr>
                <td>{{numeroItem}}</td>
                <td>{{producto}}</td>
                <td>{{version}}</td>
            </tr>
            """;

    public static final String FORM_TEMPLATE_INS_SOFTWARE = """
            <!DOCTYPE html>
            <html lang="es">
            
            <head>
                <title>{{title}}</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        font-size: 12px;
                        color: #333;
                        margin: 0;
                        padding: 0;
                    }
                    .checkbox {
                        display: inline-block;
                        width: 20px;
                        height: 20px;
                        text-align: center;
                        font-size: 18px;
                        font-weight: bold;
                        line-height: 18px;
                        vertical-align: middle;
                    }
            
                    .checkbox::before {
                        content: attr(data-content);
                    }
            
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
            
                    .label {
                        width: 25%;
                    }
            
                    .value {
                        width: 75%;
                    }
            
                    h1 {
                        font-size: 16px;
                        font-weight: bold;
                        margin-top: 20px auto;
                        padding: 4px;
                        background-color: #2D5294;
                        color: #fff;
                    }
            
                    .accesorios {
                        text-decoration: underline;
                    }
            
                    @page {
                        size: A4;
                        margin: 10mm;
                    }
                </style>
            </head>
            
            <body>
                <div>
                    <table style="border: 1px solid #ddd;">
                        <tr>
                            <td style="width: 35%; border: 1px solid #ddd;">
                                <img src='data:image/png;base64,{{ImgB64}}' alt='Logo 1' style="height: 5rem; width: auto;" />
                            </td>
                            <td style="text-align: center; width: 35%; font-weight: bold; border: 1px solid #ddd;">
                                SOLICITUD DE INSTALACION DE SOFTWARE Y HARDWARE
                            </td>
                            <td style="padding: 0;">
                                <table>
                                    <tr style="border: 1px solid #ddd;">
                                        <th style="border: 1px solid #ddd;">Código</th>
                                        <td style="border: 1px solid #ddd;">FT-GS-03</td>
                                    </tr>
                                    <tr style="border: 1px solid #ddd;">
                                        <th style="border: 1px solid #ddd;">Versión</th>
                                        <td style="border: 1px solid #ddd;">01</td>
                                    </tr>
                                    <tr style="border: 1px solid #ddd;">
                                        <th style="border: 1px solid #ddd;">Fecha</th>
                                        <td style="border: 1px solid #ddd;">02/07/2020</td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                    <h1 class="table-header">DATOS DEL COLABORADOR</h1>
                    <table border="1">
                        <tr>
                            <th class="label" style="text-align: start;" colspan="1">Apellidos y Nombres:</th>
                            <td class="value" style="text-align: start;" colspan="3">{{apellidosNombres}}</td>
                        </tr>
                        <tr>
                            <th style="text-align: start;" colspan="1">Cliente:</th>
                            <td style="text-align: start;" colspan="3">{{cliente}}</td>
                        </tr>
                        <tr>
                            <th style="text-align: start;">Área:</th>
                            <td style="text-align: start;">{{area}}</td>
                            <th style="text-align: start;">Cargo:</th>
                            <td style="text-align: start;">{{cargo}}</td>
                        </tr>
                    </table>
                    <div style="height: 16px;"></div>
                    <div>
                        <p style="display: inline-block; margin-right: 20px;">
                            Fecha de Solicitud: <span>{{fechaSolicitud}}</span>
                        </p>
                        <p style="display: inline-block;">
                            Fecha de Entrega: <span>{{fechaEntrega}}</span>
                        </p>
                    </div>
                    <div style="height: 1px; background-color: #83a9e9;"></div>
                    <h1 class="table-header">DATOS DE REQUERIMIENTO DE HARDWARE</h1>
                    <table border="1">
                        <tr>
                            <th class="table-header">TIPO</th>
                            <th class="table-header">PROCESADOR</th>
                            <th class="table-header">RAM</th>
                            <th class="table-header">HD</th>
                            <th class="table-header">MARCA(*)</th>
                        </tr>
                        <tr>
                            <td>
                                <div style="display: inline-block; margin-right: 10px;">
                                    <form>
                                        <input type="checkbox" name="check_pc" class="checkbox" readonly="true" data-content="{{symbolPc}}" />
                                        <label for="check_pc">PC</label>
                                    </form>
                                </div>
                                <div style="display: inline-block;">
                                    <form>
                                        <input type="checkbox" name="check_laptop" class="checkbox" readonly="true" data-content="{{symbolLaptop}}" />
                                        <label for="check_laptop">Laptop</label>
                                    </form>
                                </div>
                            </td>
                            <td>{{procesador}}</td>
                            <td>{{ram}}</td>
                            <td>{{hd}}</td>
                            <td>{{marca}}</td>
                        </tr>
                    </table>
            
                    <div style="height: 32px;"></div>
            
                    <table border="1">
                        <tr>
                            <th class="table-header">ANEXO</th>
                            <th class="table-header">CELULAR</th>
                            <th class="table-header">INTERNET MÓVIL</th>
                        </tr>
                        <tr>
                            <td align="center">
                                <div style="display: inline-block; margin-right: 10px;">
                                    <form>
                                        <input type="checkbox" name="check_fijo" class="checkbox" readonly="true" data-content="{{symbolFijo}}" />
                                        <label for="check_fijo">Fijo</label>
                                    </form>
                                </div>
                                <div style="display: inline-block;">
                                    <form>
                                        <input type="checkbox" name="check_softphone" class="checkbox" readonly="true" data-content="{{symbolSoftphone}}" />
                                        <label for="check_softphone">Softphone</label>
                                    </form>
                                </div>
                            </td>
                            <td align="center">
                                <div style="display: inline-block; margin-right: 10px;">
                                    <form>
                                        <input type="checkbox" name="check_cel_yes" class="checkbox" readonly="true" data-content="{{symbolCelSi}}" />
                                        <label for="check_cel_yes">Sí</label>
                                    </form>
                                </div>
                                <div style="display: inline-block;">
                                    <form>
                                        <input type="checkbox" name="check_cel_no" class="checkbox" readonly="true" data-content="{{symbolCelNo}}" />
                                        <label for="check_cel_no">No</label>
                                    </form>
                                </div>
                            </td>
                            <td align="center">
                                <div style="display: inline-block; margin-right: 10px;">
                                    <form>
                                        <input type="checkbox" name="check_internet_yes" class="checkbox" readonly="true" data-content="{{symbolIntSi}}" />
                                        <label for="check_internet_yes">Sí</label>
                                    </form>
                                </div>
                                <div style="display: inline-block;">
                                    <form>
                                        <input type="checkbox" name="check_internet_no" class="checkbox" readonly="true" data-content="{{symbolIntNo}}" />
                                        <label for="check_internet_no">No</label>
                                    </form>
                                </div>
                            </td>
                        </tr>
                    </table>
                    <div style="height: 32px;"></div>
                    <p>Accesorios: <span class="accesorios">{{accesorios}}</span></p>
                    <h1 class="table-header">DATOS DE LA INSTALACION DE SOFTWARE</h1>
                    <table border="1">
                        <tr>
                            <th class="table-header">ITEM</th>
                            <th class="table-header">PRODUCTO</th>
                            <th class="table-header">VERSION</th>
                        </tr>
                        {{listaProducto}}
                    </table>
            
                    <div style="height: 100px;"></div>
                    <div style="text-align: center;">
                        <p style="font-weight: bold;">{{nombreFirma}}</p>
                        <div style="height: 1px; width: 280px; background-color: #83a9e9; margin: auto;"></div>
                        <p style="font-weight: bold;">{{nombreGestor}}</p>
                        <p style="font-weight: bold;">Gestor de Servicio</p>
                        <p style="font-weight: bold;">Gerencia de Operaciones</p>
                    </div>
                </div>
            </body>
            
            </html>
            """;

    public static final String FORM_TEMPLATE_SOLICITUD = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <title>{{title}}</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        font-size: 12px;
                        color: #333;
                        margin: 0;
                        padding: 0;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                    }
                    td,
                    th {
                        padding: 5px;
                        text-align: left;
                    }
                    .label {
                        width: 25%;
                    }
                    .value {
                        width: 75%;
                    }
                    h1 {
                        font-size: 20px;
                        font-weight: bold;
                        margin-top: 20px;
                        padding: 4px;
                        background-color: #2D5294;
                        color: #fff;
                    }
                    @page {
                        size: A4;
                        margin: 10mm;
                    }
                </style>
            </head>
            <body>
                <div>
                    <table>
                        <tr>
                            <td style="width: 35%; border: 1px solid #ddd;">
                                <img src='data:image/png;base64,{{ImgB64}}' alt='Logo 1' style="height: 5rem; width: auto;" />
                            </td>
                            <td style="text-align: center; width: 35%; font-weight: bold; border: 1px solid #ddd;">
                                SOLICITUD DE CREACIÓN, MODIFICACIÓN O DESACTIVACIÓN DE USUARIOS
                            </td>
                            <td style="padding: 0;">
                                <table>
                                    <tr style="border: 1px solid #ddd;">
                                        <th style="border: 1px solid #ddd;">Código</th>
                                        <td style="border: 1px solid #ddd;">FT-GS-01</td>
                                    </tr>
                                    <tr style="border: 1px solid #ddd;">
                                        <th style="border: 1px solid #ddd;">Versión</th>
                                        <td style="border: 1px solid #ddd;">01</td>
                                    </tr>
                                    <tr style="border: 1px solid #ddd;">
                                        <th style="border: 1px solid #ddd;">Fecha</th>
                                        <td style="border: 1px solid #ddd;">31/08/2016</td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                    <h1>DATOS DEL SOLICITANTE</h1>
                    <table>
                        <tr>
                            <th class="label">Nombres y Apellidos:</th>
                            <td class="value">{{solicitante}}</td>
                        </tr>
                        <tr>
                            <th>Área:</th>
                            <td>{{area}}</td>
                        </tr>
                        <tr>
                            <th>Anexo:</th>
                            <td>102</td>
                        </tr>
                        <tr>
                            <th>Fecha:</th>
                            <td>{{fechaSolicitud}}</td>
                        </tr>
                    </table>
                    <h1>CREACIÓN DE USUARIOS</h1>
                    <table>
                        <tr>
                            <th class="label">Nombres y Apellidos:</th>
                            <td>{{nombresCreacion}} {{apellidosCreacion}}</td>
                        </tr>
                        <tr>
                            <th class="label">Nombre de Usuario:</th>
                            <td>{{nombreUsuarioCreacion}}</td>
                        </tr>
                        <tr>
                            <th class="label">Correo:</th>
                            <td>{{correoCreacion}}</td>
                        </tr>
                        <tr>
                            <th class="label">Área:</th>
                            <td>{{areaCreacion}}</td>
                        </tr>
                        <tr>
                            <td>
                                <form>
                                    <label for="entrada">Entrada:</label>
                                    <input type="checkbox" name="entrada"/>
                                    <label for="salida">Salida:</label>
                                    <input type="checkbox" name="salida"/>
                                </form>
                            </td>
                            <td>
                                <form>
                                    <label for="interno">Interno:</label>
                                    <input type="checkbox" name="interno"/>
                                    <label for="externo">Externo:</label>
                                    <input type="checkbox" name="externo" />
                                </form>
                            </td>
                        </tr>
                    </table>
                    <h1>MODIFICACIÓN DE USUARIOS</h1>
                    <table>
                        <tr>
                            <td>
                                <form>
                                    <label for="mod_usuario">Modificar Usuario de Red:</label>
                                    <input type="checkbox" name="mod_usuario"/>
                                </form>
                            </td>
                        </tr>
                        <tr>
                            <th class="label">Usuario:</th>
                            <td>{{usuarioActualModificacion}}</td>
                        </tr>
                        <tr style="border-bottom: 1px solid #ddd;">
                            <th class="label">Modificar por:</th>
                            <td>{{usuarioNuevoModificacion}}</td>
                        </tr>
                        <tr>
                            <td>
                                <form>
                                    <label for="mod_email">Modificar Correo:</label>
                                    <input type="checkbox" name="mod_email" />
                                </form>
                            </td>
                        </tr>
                        <tr>
                            <th class="label">Correo:</th>
                            <td>{{correoActualModificacion}}</td>
                        </tr>
                        <tr>
                            <th class="label">Modificar por:</th>
                            <td>{{correoNuevoModificacion}}</td>
                        </tr>
                    </table>
                    <h1>DESACTIVACIÓN DE USUARIOS</h1>
                    <table>
                        <tr>
                            <th class="label">Nombres y Apellidos:</th>
                            <td>{{nombresCese}} {{apellidosCese}}</td>
                        </tr>
                        <tr>
                            <th class="label">Usuario:</th>
                            <td>{{usuarioCese}}</td>
                        </tr>
                        <tr>
                            <th class="label">Correo:</th>
                            <td>{{correoCese}}</td>
                        </tr>
                        <tr>
                            <th class="label">Motivo:</th>
                            <td>{{motivoCese}}</td>
                        </tr>
                    </table>
                    <table style="margin-top: 50px;">
                        <tr>
                            <td style="width: 100%; text-align: center; ">
                                <p style="text-decoration: underline; line-height: 0%; font-size: medium;">{{firmante}}</p>
                                <p style="font-weight: bold; font-size: medium;">GESTOR DEL SERVICIO</p>
                            </td>
                        </tr>
                    </table>
                </div>
            </body>
            </html>
            """;
    public static final String FORM_TEMPLATE_EMPLOYEE = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <title>{{title}}</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        font-size: 12px;
                        color: #333;
                        margin: 0;
                        padding: 0;
                    }
                    h1 {
                        font-size: 16px;
                        color: #333;
                        margin: 10px 0;
                    }
                    p {
                        margin: 5px 0;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                    }
                    td {
                        border: 1px solid #ddd;
                    }
                    .label {
                        background-color: #e0dfdf;
                    }
                    th, td {
                        width: 30%;
                        padding: 5px;
                        text-align: left;
                    }
                    table.inner-table {
                        table-layout: fixed;
                        width: 100%;
                    }
                    table.inner-table th,
                    table.inner-table td {
                        width: 33%;
                        border: 1px solid #ddd;
                        padding: 5px;
                    }
                    table.inner-table td {
                        border-left: none;
                        border-bottom: none;
                    }
                    .no-page-break {
                        page-break-inside: avoid;
                    }
                    .no-page-break tr {
                        page-break-inside: avoid;
                    }
                    .no-page-break td {
                        page-break-inside: avoid;
                    }
                    @page {
                        size: A4;
                        margin: 10mm;
                    }
                    .content-container {
                        max-width: 100%;
                        overflow: hidden;
                    }
                </style>
            </head>
            <body>
                <div class="content-container">
                    <table>
                        <tr>
                            <td style="width: 35%; text-align: center;">
                                <img src='data:image/png;base64,{{ImgB64}}' alt='Logo 1' style="height: 5rem; width: auto;" />
                            </td>
                            <td style="text-align: center; width: 35%; font-weight: bold;">FORMULARIO DE MOVIMIENTO</td>
                            <td style='width: 35%; padding: 0;'>
                                <table style='padding: 0; margin: 0;'>
                                    <tr>
                                        <td>Código</td>
                                        <td>FT-GTH-12</td>
                                    </tr>
                                    <tr>
                                        <td>Versión</td>
                                        <td>02</td>
                                    </tr>
                                    <tr>
                                        <td>Fecha</td>
                                        <td>01/12/2021</td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
            
                    <h1>DATOS DEL COLABORADOR</h1>
                    <table class="no-page-break">
                        <tr>
                            <th class="label">Nombres y Apellidos</th>
                            <td>{{nombres}} {{apellidos}}</td>
                        </tr>
                        <tr>
                            <th class="label">Unidad</th>
                            <td>{{unidad}}</td>
                        </tr>
                    </table>
            
                    <h1>INGRESO</h1>
                    <table class="no-page-break">
                        <tr>
                            <th class="label">Modalidad</th>
                            <td>{{modalidad}}</td>
                        </tr>
                        <tr>
                            <th class="label">Motivo de Ingreso</th>
                            <td>{{motivoIngreso}}</td>
                        </tr>
                        <tr>
                            <th class="label">Cargo</th>
                            <td>{{cargo}}</td>
                        </tr>
                        <tr>
                            <th class="label">Horario de Trabajo</th>
                            <td>{{horarioTrabajo}}</td>
                        </tr>
                        <tr>
                            <th class="label">Estructura Salarial</th>
                            <td style="padding: 0;">
                                <table class="inner-table">
                                    <tr>
                                        <th>Base</th>
                                        <th>Movilidad</th>
                                        <th>Bono Trimestral</th>
                                    </tr>
                                    <tr>
                                        <td>{{montoBaseIn}}</td>
                                        <td>{{montoMovilidadIn}}</td>
                                        <td>{{montoTrimestralIn}}</td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                        <tr>
                            <th class="label">F. Inicio contrato</th>
                            <td>{{fechaInicioContrato}}</td>
                        </tr>
                        <tr>
                            <th class="label">F. Término contrato</th>
                            <td>{{fechaTerminoContrato}}</td>
                        </tr>
                        <tr>
                            <th class="label">Proyecto / servicio</th>
                            <td>{{proyectoServicio}}</td>
                        </tr>
                        <tr>
                            <th class="label">Objeto del contrato</th>
                            <td>{{objetoContrato}}</td>
                        </tr>
                    </table>
            
                    <h1>DECLARACIÓN EN SUNAT (*)</h1>
                    <table class="no-page-break">
                        <tr>
                            <th class="label">[1] ¿Declarado en Sunat?</th>
                            <td>{{declaradoSunat}}</td>
                        </tr>
                        <tr>
                            <th class="label">[2] Sede a declarar</th>
                            <td>{{sedeDeclarar}}</td>
                        </tr>
                    </table>
            
                    <h1>MOVIMIENTO</h1>
                    <table class="no-page-break">
                        <tr>
                            <th class="label">Estructura Salarial</th>
                            <td style="padding: 0;">
                                <table class="inner-table">
                                    <tr>
                                        <th>Base</th>
                                        <th>Movilidad</th>
                                        <th>Bono Trimestral</th>
                                    </tr>
                                    <tr>
                                        <td>{{montoBaseMov}}</td>
                                        <td>{{montoMovilidadMov}}</td>
                                        <td>{{montoTrimestralMov}}</td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                        <tr>
                            <th class="label">Cambio de puesto</th>
                            <td>{{puesto}}</td>
                        </tr>
                        <tr>
                            <th class="label">Cambio de área</th>
                            <td>{{area}}</td>
                        </tr>
                        <tr>
                            <th class="label">Cambio de jornada</th>
                            <td>{{jornada}}</td>
                        </tr>
                        <tr>
                            <th class="label">F. Inicio de movimiento</th>
                            <td>{{fechaMovimiento}}</td>
                        </tr>
                    </table>
            
                    <h1>CESE</h1>
                    <table style="margin-bottom: 10px;">
                        <tr>
                            <th class="label">Motivo de Cese</th>
                            <td>{{motivoCese}}</td>
                        </tr>
                        <tr>
                            <th class="label">Fecha de Cese</th>
                            <td>{{fechaCese}}</td>
                        </tr>
                    </table>
            
                    <table style="margin-bottom: 10px;">
                        <tr style="border: none;">
                            <td style="width: 100%; text-align: center; border: none;">
                                <img src='data:image/png;base64,{{ImgFirma}}' alt='Firma' style="height: 5rem; width: auto;" />
                                <p style="margin-top:-10px;">_________________________________________</p>
                                <p>{{firmante}}</p>
                                <p>Gestor de Servicios</p>
                            </td>
                        </tr>
                    </table>
            
                    <p style="text-align: justify; font-size: 10px;">
                        (*) DECLARACION EN SUNAT - En esta sección deberá informar al colaborador que se encuentra en Planilla
                        y será declarado en SUNAT. Responda la pregunta [1], en caso su respuesta sea afirmativa, por favor
                        responda a la pregunta [2] tener en cuenta que si la respuesta es "Oficina del Cliente" se debe
                        contar con un contrato entre empresas.
                    </p>
                </div>
            </body>
            </html>
            """;
}
