package org.app.autfmi;

import jakarta.mail.MessagingException;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.PDFUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AutfmiendpointApplication implements CommandLineRunner {

    @Autowired
    private Common utilitarios;

    @Autowired
    private PDFUtils pdfUtils;

    public static void main(String[] args) {
        SpringApplication.run(AutfmiendpointApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        try {
            String imgb64 = pdfUtils.loadLogoPDF(1);

            String htmlTemplate = """
                        <!DOCTYPE html>
                        <html lang="es">
                        <head>
                            <title>PDF de Prueba</title>
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
                                    margin-bottom: 10px;
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
                                .logo {
                                    width: auto;
                                    display: block;
                                    font-size: 30px;
                                    margin: auto;
                                }
                                .fractal {
                                    display: block;
                                }
                                .letter {
                                    color: #0B85C3;
                                    font-weight: bold;
                                }
                                .triangle {
                                    width: 0;
                                    height: 0;
                                    border-left: 13px solid transparent;
                                    border-right: 13px solid transparent;
                                    border-bottom: 23px solid #faab34;
                                    display: inline-block;
                                    position: relative;
                                }
                                .inner-triangle {
                                    width: 0;
                                    height: 0;
                                    border-left: 7px solid transparent;
                                    border-right: 7px solid transparent;
                                    border-bottom: 13px solid white;
                                    position: absolute;
                                    top: 7px;
                                    left: -7px;
                                }
                                .subtitle {
                                    font-size: 16px;
                                    color: #808080;
                                    margin-left: 6rem;
                                    text-align: center;
                                }
                                .logo-container {
                                    width: auto;
                                    max-width: 100%;
                                    margin-right: 20px;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="content-container">
                                <table>
                                    <tr>
                                        <td style="width: 35%; text-align: center;">
                                            <div class="logo-container">
                                                <img src='data:image/png;base64,{{ImgB64}}' alt='Logo 1'/>
                                            </div>
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
                                                    <td>{{fecha}}</td>
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
                                                    <td>{{montoBase}}</td>
                                                    <td>{{montoMovilidad}}</td>
                                                    <td>{{montoTrimestral}}</td>
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
                                                    <td>{{montoBase}}</td>
                                                    <td>{{montoMovilidad}}</td>
                                                    <td>{{montoTrimestral}}</td>
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
                                <table style="margin-bottom: 50px;">
                                    <tr>
                                        <th class="label">Motivo de Cese</th>
                                        <td>{{motivoCese}}</td>
                                    </tr>
                                    <tr>
                                        <th class="label">Fecha de Cese</th>
                                        <td>{{fechaCese}}</td>
                                    </tr>
                                </table>

                                <table style="margin-bottom: 20px;">
                                    <tr style="border: none;">
                                        <td style="width: 100%; text-align: center; border: none;">
                                            <p>_________________________________________</p>
                                            <p>{{firmante}}</p>
                                            <p>{{Gestor de Servicios}}</p>
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

            htmlTemplate = htmlTemplate.replace("{{ImgB64}}", imgb64);

//            String htmlConDatos = utilitarios.reemplazarValores(htmlTemplate, nombre, saldo);

            utilitarios.enviarCorreoConPDF(
                    htmlTemplate,
                    "dial1901110514@gmail.com",
                    "Prueba correo",
                    """
                            Hola\s

                            Se realizó una actualización del talento, por favor revise el documento."""
            );
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Error al enviar el correo: " + e.getMessage());
        }
    }
}
