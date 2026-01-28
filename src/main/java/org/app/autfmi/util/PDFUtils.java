package org.app.autfmi.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.report.*;
import org.app.autfmi.model.request.SolicitudSoftwareRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class PDFUtils {

    public enum TemplateType {
        SOLICITUD,
        SOLICITUD_EQUIPO,
        FORMULARIO, // PARA INGRESO MOVIMIENTO Y CESE
    }

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emisorCorreo;

    public String loadImage(String pathImage) {
        String base64Image = "";

        Resource resource = new ClassPathResource(pathImage);

        try (InputStream inputStream = resource.getInputStream()) {
            byte[] data = inputStream.readAllBytes();
            base64Image = Base64.getEncoder().encodeToString(data);
        } catch (IOException e) {
            System.out.println("Error al cargar la imagen PDF en la ruta: " + pathImage + " :   " + e.getMessage());
        }

        return base64Image;
    }

    public byte[] crearPDF(String htmlContent, String title) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            htmlContent = htmlContent.replace("{{title}}", title);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(htmlContent, "file:///");
            builder.toStream(baos);
            builder.run();

            return baos.toByteArray();
        } catch (Exception e) {
            System.out.println("Error al generar PDF: " + e.getMessage());
            return null;
        } finally {
            try {
                baos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String filePDFToBase64(byte[] byteArchivo) {
        if (byteArchivo == null || byteArchivo.length == 0) {
            throw new IllegalArgumentException("Archivo nulo");
        }

        return Base64.getEncoder().encodeToString(byteArchivo);
    }

    public void enviarCorreoConPDF(List<FileDTO> lstfiles,
            @NonNull String to,
            @NonNull List<String> copyTo,
            @NonNull String subject,
            @NonNull String text)
            throws MessagingException {
        try {
            for (FileDTO lstfile : lstfiles) {
                lstfile.setByteArchivo(crearPDF(lstfile.getHtmlTemplate(), ""));
            }
        } catch (Exception e) {
            throw new MessagingException("Error al generar el PDF", e);
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(emisorCorreo != null ? emisorCorreo : "");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);

        if (copyTo != null && !copyTo.isEmpty()) {
            copyTo.removeIf(email -> email.equals(to));

            if (!copyTo.isEmpty()) {
                helper.setCc(copyTo.toArray(new String[0]));
            }
        }

        for (FileDTO objfile : lstfiles) {
            ByteArrayDataSource dataSource = new ByteArrayDataSource(objfile.getByteArchivo(), "application/pdf");
            helper.addAttachment(objfile.getNombreArchivo() + ".pdf", dataSource);
        }

        mailSender.send(message);
    }

    public String getHtmlTemplate(TemplateType reportType) {
        String imageB64 = switch (reportType) {
            case SOLICITUD, SOLICITUD_EQUIPO -> loadImage("assets/logo-fractal-2.png");
            case FORMULARIO -> loadImage("assets/logo-fractal.png");
        };

        return switch (reportType) {
            case SOLICITUD -> Constante.FORM_TEMPLATE_SOLICITUD.replace("{{ImgB64}}", imageB64);
            case FORMULARIO -> Constante.FORM_TEMPLATE_EMPLOYEE.replace("{{ImgB64}}", imageB64);
            case SOLICITUD_EQUIPO -> Constante.FORM_TEMPLATE_INS_SOFTWARE.replace("{{ImgB64}}", imageB64);
        };
    }

    /**
     * Reemplaza los valores del reporte de ingreso en la plantilla HTML
     * 
     * @param htmlTemplate La plantilla HTML con marcadores de posición
     * @param report       El objeto EntryReport que contiene los datos a insertar
     * @param gs           El objeto GestorDTO que contiene información del gestor
     * @return La plantilla HTML con los valores reemplazados
     */
    public String replaceEntryRequestValues(String htmlTemplate, EntryReport report, GestorDTO gs) {
        htmlTemplate = htmlTemplate
                .replace("{{title}}", "FT-GT-12 Formulario de Ingreso")
                // DATOS COLABORADOR
                .replace("{{nombres}}", SafeValues.safeString(report.getNombres()))
                .replace("{{apellidos}}", SafeValues.safeString(report.getApellidos()))
                .replace("{{unidad}}", SafeValues.safeString(report.getUnidad()))
                // INGRESO
                .replace("{{modalidad}}", SafeValues.safeString(report.getModalidad()))
                .replace("{{motivoIngreso}}", SafeValues.safeString(report.getMotivo()))
                .replace("{{cargo}}", SafeValues.safeString(report.getCargo()))
                .replace("{{horarioTrabajo}}", SafeValues.safeString(report.getHorario()))
                .replace("{{montoBaseIn}}", SafeValues.safeString(report.getMontoBase()))
                .replace("{{montoMovilidadIn}}", SafeValues.safeString(report.getMontoMovilidad()))
                .replace("{{montoTrimestralIn}}", SafeValues.safeString(report.getMontoTrimestral()))
                .replace("{{fechaInicioContrato}}", SafeValues.safeString(report.getFechaInicioContrato()))
                .replace("{{fechaTerminoContrato}}", SafeValues.safeString(report.getFechaFinContrato()))
                .replace("{{proyectoServicio}}", SafeValues.safeString(report.getProyectoServicio()))
                .replace("{{objetoContrato}}", SafeValues.safeString(report.getObjetoContrato()))
                // SUNAT
                .replace("{{declaradoSunat}}", SafeValues.safeString(report.getDeclararSunat() == 1 ? "Si" : "No"))
                .replace("{{sedeDeclarar}}", SafeValues.safeString(report.getSedeDeclararSunat()))
                // MOVIMIENTO
                .replace("{{montoBaseMov}}", "Monto")
                .replace("{{montoMovilidadMov}}", "Monto")
                .replace("{{montoTrimestralMov}}", "Monto")
                .replace("{{puesto}}", "Escribir el nuevo puesto")
                .replace("{{area}}", "Escribir la nueva área")
                .replace("{{jornada}}", "Escribir la nueva jornada")
                .replace("{{fechaMovimiento}}", "Escribir la fecha de movimiento")
                // CESE
                .replace("{{motivoCese}}", "Escribir el motivo de cese")
                .replace("{{fechaCese}}", "Escribir el fecha de cese")
                .replace("{{fechaDevolucionEquipo}}", "Escribir la fecha de devolución de equipo")
                // FOOTER
                .replace("{{nombreFirma}}", SafeValues.safeString(gs.getFullname()))
                .replace("{{firmante}}", SafeValues.safeString(gs.getFullname()))
                .replace("{{fechaEmision}}", Common.getCurrentDateFormatted());

        return htmlTemplate;
    }

    /**
     * Reemplaza los valores del reporte de movimiento en la plantilla HTML
     * 
     * @param htmlTemplate La plantilla HTML con marcadores de posición
     * @param report       El objeto MovementReport que contiene los datos a
     *                     insertar
     * @return La plantilla HTML con los valores reemplazados
     */
    public String replaceMovementRequestValues(String htmlTemplate, MovementReport report) {
        htmlTemplate = htmlTemplate
                .replace("{{title}}", "FT-GT-12 Formulario de Movimiento")
                // DATOS COLABORADOR
                .replace("{{nombres}}", report.getNombres())
                .replace("{{apellidos}}", report.getApellidos())
                .replace("{{unidad}}", report.getUnidad())
                // INGRESO
                .replace("{{modalidad}}", "Escriba la modalidad")
                .replace("{{motivoIngreso}}", "Escriba el motivo de ingreso")
                .replace("{{cargo}}", "Escriba el cargo")
                .replace("{{horarioTrabajo}}", "Escriba el horario de trabajo")
                .replace("{{montoBaseIn}}", "Monto")
                .replace("{{montoMovilidadIn}}", "Monto")
                .replace("{{montoTrimestralIn}}", "Monto")
                .replace("{{fechaInicioContrato}}", "Escriba la fecha de inicio de contrato")
                .replace("{{fechaTerminoContrato}}", "Escriba el fecha de termino de contrato")
                .replace("{{proyectoServicio}}", "Escriba el proyecto ó servicio")
                .replace("{{objetoContrato}}", "Escriba el objeto contrato")
                // SUNAT
                .replace("{{declaradoSunat}}", "")
                .replace("{{sedeDeclarar}}", "Escriba la sede")
                // MOVIMIENTO
                .replace("{{montoBaseMov}}", report.getMontoBase().toString())
                .replace("{{montoMovilidadMov}}", report.getMontoMovilidad().toString())
                .replace("{{montoTrimestralMov}}", report.getMontoTrimestral().toString())
                .replace("{{puesto}}", SafeValues.safeString(report.getPuesto()))
                .replace("{{area}}", SafeValues.safeString(report.getArea()))
                .replace("{{jornada}}", SafeValues.safeString(report.getHorario()))
                .replace("{{fechaMovimiento}}", report.getFechaHistorial())
                // CESE
                .replace("{{motivoCese}}", "Escribir el motivo de cese")
                .replace("{{fechaCese}}", "Escribir el fecha de cese")
                .replace("{{fechaDevolucionEquipo}}", "")
                // FOOTER
                .replace("{{nombreFirma}}", SafeValues.safeString(report.getFirmante()))
                .replace("{{firmante}}", SafeValues.safeString(report.getFirmante()))
                .replace("{{fechaEmision}}", Common.getCurrentDateFormatted());
        return htmlTemplate;
    }

    /**
     * Reemplaza los valores del reporte de cese en la plantilla HTML
     * 
     * @param htmlTemplate La plantilla HTML con marcadores de posición
     * @param report       El objeto CeseReport que contiene los datos a insertar
     * @param gs           El objeto GestorDTO que contiene información del gestor
     * @return La plantilla HTML con los valores reemplazados
     */
    public String replaceOutRequestValues(
            String htmlTemplate,
            CeseReport report,
            @NonNull GestorDTO gs) {

        htmlTemplate = htmlTemplate
                .replace("{{title}}", "FT-GT-12 Formulario de Cese")
                // DATOS COLABORADOR
                .replace("{{nombres}}", report.getNombres())
                .replace("{{apellidos}}", report.getApellidos())
                .replace("{{unidad}}", report.getUnidad())
                // INGRESO
                .replace("{{modalidad}}", "Escriba la modalidad")
                .replace("{{motivoIngreso}}", "Escriba el motivo de ingreso")
                .replace("{{cargo}}", "Escriba el cargo")
                .replace("{{horarioTrabajo}}", "Escriba el horario de trabajo")
                .replace("{{montoBaseIn}}", "Monto")
                .replace("{{montoMovilidadIn}}", "Monto")
                .replace("{{montoTrimestralIn}}", "Monto")
                .replace("{{fechaInicioContrato}}", "Escriba la fecha de inicio de contrato")
                .replace("{{fechaTerminoContrato}}", "Escriba el fecha de termino de contrato")
                .replace("{{proyectoServicio}}", "Escriba el proyecto ó servicio")
                .replace("{{objetoContrato}}", "Escriba el objeto contrato")
                // SUNAT
                .replace("{{declaradoSunat}}", "")
                .replace("{{sedeDeclarar}}", "Escriba la sede")
                // MOVIMIENTO
                .replace("{{montoBaseMov}}", "Monto")
                .replace("{{montoMovilidadMov}}", "Monto")
                .replace("{{montoTrimestralMov}}", "Monto")
                .replace("{{puesto}}", "Escribir el nuevo puesto")
                .replace("{{area}}", "Escribir la nueva área")
                .replace("{{jornada}}", "Escribir la nueva jornada")
                .replace("{{fechaMovimiento}}", "Escribir la fecha de movimiento")
                // CESE
                .replace("{{motivoCese}}", SafeValues.safeString(report.getMotivo()))
                .replace("{{fechaCese}}", SafeValues.safeString(report.getFechaHistorial()))
                .replace("{{fechaDevolucionEquipo}}", SafeValues.safeString(report.getFchDevolucionEquipo()))
                // FOOTER
                .replace("{{nombreFirma}}", SafeValues.safeString(gs.getFullname()))
                .replace("{{firmante}}", SafeValues.safeString(gs.getFullname()))
                .replace("{{fechaEmision}}", Common.getCurrentDateFormatted());
        return htmlTemplate;
    }

    /**
     * Reemplaza los valores del reporte de solicitud en la plantilla HTML
     * 
     * @param htmlTemplate La plantilla HTML con marcadores de posición
     * @param data         El objeto SolicitudData que contiene los datos a insertar
     * @param gs           El objeto GestorDTO que contiene información del gestor
     * @return La plantilla HTML con los valores reemplazados
     */
    public String replaceSolicitudPDFValues(String htmlTemplate, SolicitudData data, GestorDTO gs) {
        htmlTemplate = htmlTemplate
                // DATOS DEL SOLICITANTE
                .replace("{{solicitante}}", data.getNombres() == null ? "" : data.getFirmante())
                .replace("{{area}}", data.getArea() == null ? "" : data.getArea())
                .replace("{{fechaSolicitud}}", data.getFechaSolicitud() == null ? "" : data.getFechaSolicitud())

                // CREACIÓN DE USUARIOS
                .replace("{{nombresCreacion}}", data.getNombresCreacion() == null ? "" : data.getNombresCreacion())
                .replace("{{apellidosCreacion}}",
                        data.getApellidosCreacion() == null ? "" : data.getApellidosCreacion())
                .replace("{{nombreUsuarioCreacion}}",
                        data.getNombreUsuarioCreacion() == null ? "" : data.getNombreUsuarioCreacion())
                .replace("{{correoCreacion}}", data.getCorreoCreacion() == null ? "" : data.getCorreoCreacion())
                .replace("{{areaCreacion}}", data.getAreaCreacion() == null ? "" : data.getAreaCreacion())

                // MODIFICACIÓN DE USUARIOS
                .replace("{{usuarioActualModificacion}}",
                        data.getUsuarioActualModificacion() == null ? "" : data.getUsuarioActualModificacion())
                .replace("{{usuarioNuevoModificacion}}",
                        data.getUsuarioNuevoModificacion() == null ? "" : data.getUsuarioNuevoModificacion())
                .replace("{{correoActualModificacion}}",
                        data.getCorreoActualModificacion() == null ? "" : data.getCorreoActualModificacion())
                .replace("{{correoNuevoModificacion}}",
                        data.getCorreoNuevoModificacion() == null ? "" : data.getNombres())

                // DESACTIVACIÓN DE USUARIOS
                .replace("{{nombresCese}}", data.getNombresCese() == null ? "" : data.getNombresCese())
                .replace("{{apellidosCese}}", data.getApellidosCese() == null ? "" : data.getApellidosCese())
                .replace("{{usuarioCese}}", data.getUsuarioCese() == null ? "" : data.getUsuarioCese())
                .replace("{{correoCese}}", data.getCorreoCese() == null ? "" : data.getCorreoCese())
                .replace("{{motivoCese}}", data.getMotivoCese() == null ? "" : data.getMotivoCese())

                // FOOTER
                .replace("{{nombreFirma}}", SafeValues.safeString(gs.getFullname()))
                .replace("{{fechaEmision}}", Common.getCurrentDateFormatted());

        return htmlTemplate;
    }

    /**
     * Reemplaza los valores del reporte de solicitud de equipo en la plantilla HTML
     *
     * @param htmlTemplate La plantilla HTML con marcadores de posición
     * @param report       El objeto SolicitudEquipoReport que contiene los datos a
     *                     insertar
     * @param gs           El objeto GestorDTO que contiene información del gestor
     * @return La plantilla HTML con los valores reemplazados
     */

    public String replaceSolicitudEquipoPDFValues(String htmlTemplate, SolicitudEquipoReport report,
            @NonNull GestorDTO gs) {
        String nombresApellidos = report.getNombreEmpleado() + ' ' + report.getApellidosEmpleado();
        String checkboxCheckedSymbol = "X";

        String tipoPc = report.getIdTipoEquipo() == 1 ? checkboxCheckedSymbol : "";
        String tipoLaptop = report.getIdTipoEquipo() == 2 ? checkboxCheckedSymbol : "";
        String anexoFijo = report.getIdAnexo() == 1 ? checkboxCheckedSymbol : "";
        String anexoSoftphone = report.getIdAnexo() == 2 ? checkboxCheckedSymbol : "";
        String celularSi = report.getCelular() ? checkboxCheckedSymbol : "";
        String celularNo = !report.getCelular() ? checkboxCheckedSymbol : "";
        String internetSi = report.getInternetMovil() ? checkboxCheckedSymbol : "";
        String internetNo = !report.getInternetMovil() ? checkboxCheckedSymbol : "";

        // lista de productos
        List<String> listaProductos = new ArrayList<>();

        if (report.getLstSoftware() != null) {
            for (int i = 0; i < report.getLstSoftware().size(); i++) {
                SolicitudSoftwareRequest requestSoftware = report.getLstSoftware().get(i);

                listaProductos.add(Constante.LIST_ITEM
                        .replace("{{numeroItem}}", String.valueOf(i + 1))
                        .replace("{{producto}}", requestSoftware.getProducto())
                        .replace("{{version}}", requestSoftware.getProdVersion()));
            }
        }

        htmlTemplate = htmlTemplate
                .replace("{{title}}", "FT-GS-03 Formulario de Requerimiento de Software y Hardware")
                .replace("{{apellidosNombres}}", nombresApellidos)
                .replace("{{cliente}}", report.getCliente())
                .replace("{{area}}", report.getArea())
                .replace("{{cargo}}", report.getPuesto())
                .replace("{{fechaSolicitud}}", Common.parseDateToFormDate(report.getFechaSolicitud()))
                .replace("{{fechaEntrega}}", Common.parseDateToFormDate(report.getFechaEntrega()))
                .replace("{{symbolPc}}", tipoPc)
                .replace("{{symbolLaptop}}", tipoLaptop)
                .replace("{{procesador}}", report.getProcesador())
                .replace("{{ram}}", report.getRam())
                .replace("{{hd}}", report.getHd())
                .replace("{{marca}}", report.getMarca())
                .replace("{{symbolFijo}}", anexoFijo)
                .replace("{{symbolSoftphone}}", anexoSoftphone)
                .replace("{{symbolCelSi}}", celularSi)
                .replace("{{symbolCelNo}}", celularNo)
                .replace("{{symbolIntSi}}", internetSi)
                .replace("{{symbolIntNo}}", internetNo)
                .replace("{{accesorios}}", report.getAccesorios())
                .replace("{{listaProducto}}", String.join("\n", listaProductos))
                .replace("{{nombreFirma}}", SafeValues.safeString(gs.getSignature()))
                .replace("{{nombreGestor}}", SafeValues.safeString(gs.getFullname()))
                .replace("{{fechaEmision}}", Common.getCurrentDateFormatted());

        return htmlTemplate;
    }
}
