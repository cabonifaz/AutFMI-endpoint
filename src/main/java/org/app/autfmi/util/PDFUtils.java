package org.app.autfmi.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.report.SolicitudData;
import org.app.autfmi.model.request.SolicitudEquipoRequest;
import org.app.autfmi.model.request.SolicitudSoftwareRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDate;
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

    public byte[] crearPDF(String htmlContent, String title ) {
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

    public void enviarCorreoConPDF(List<FileDTO> lstfiles, String to, String subject, String text) throws MessagingException {
        try {
            for (FileDTO lstfile : lstfiles) {
                lstfile.setByteArchivo(crearPDF(lstfile.getHtmlTemplate(), ""));
            }
        } catch (Exception e) {
            throw new MessagingException("Error al generar el PDF", e);
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(emisorCorreo);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);

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

    public String replaceEntryRequestValues(String htmlTemplate, EntryReport report) {
        htmlTemplate = htmlTemplate
                .replace("{{title}}", "FT-GT-12 Formulario de Ingreso")
                // DATOS COLABORADOR
                .replace("{{nombres}}", report.getNombres())
                .replace("{{apellidos}}", report.getApellidos())
                .replace("{{unidad}}", report.getUnidad())
                // INGRESO
                .replace("{{modalidad}}", report.getModalidad())
                .replace("{{motivoIngreso}}", report.getMotivo())
                .replace("{{cargo}}", report.getCargo())
                .replace("{{horarioTrabajo}}", report.getHorarioTrabajo())
                .replace("{{montoBaseIn}}", report.getMontoBase().toString())
                .replace("{{montoMovilidadIn}}", report.getMontoMovilidad().toString())
                .replace("{{montoTrimestralIn}}", report.getMontoTrimestral().toString())
                .replace("{{fechaInicioContrato}}", report.getFechaInicioContrato())
                .replace("{{fechaTerminoContrato}}", report.getFechaFinContrato())
                .replace("{{proyectoServicio}}", report.getProyectoServicio())
                .replace("{{objetoContrato}}", report.getObjetoContrato())
                // SUNAT
                .replace("{{declaradoSunat}}", report.getDeclararSunat() == 1 ? "Si" : "No")
                .replace("{{sedeDeclarar}}", report.getSedeDeclararSunat())
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
                // FOOTER
                .replace("{{nombreFirma}}", report.getFirmante())
                .replace("{{firmante}}", report.getFirmante());

        return htmlTemplate;
    }

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
                .replace("{{puesto}}", report.getPuesto())
                .replace("{{area}}", report.getArea())
                .replace("{{jornada}}", report.getJornada())
                .replace("{{fechaMovimiento}}", report.getFechaHistorial())
                // CESE
                .replace("{{motivoCese}}", "Escribir el motivo de cese")
                .replace("{{fechaCese}}", "Escribir el fecha de cese")
                // FOOTER
                .replace("{{nombreFirma}}", report.getFirmante())
                .replace("{{firmante}}", report.getFirmante());
        return htmlTemplate;
    }

    public String replaceOutRequestValues(String htmlTemplate, CeseReport report) {
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
                .replace("{{motivoCese}}", report.getMotivo())
                .replace("{{fechaCese}}", report.getFechaHistorial())
                // FOOTER
                .replace("{{nombreFirma}}", report.getFirmante())
                .replace("{{firmante}}", report.getFirmante());
        return htmlTemplate;
    }

    public String replaceSolicitudPDFValues(String htmlTemplate, SolicitudData report) {
        htmlTemplate = htmlTemplate
                //DATOS DEL SOLICITANTE
                .replace("{{solicitante}}", report.getNombres() == null ? "" : report.getFirmante())
                .replace("{{area}}", report.getArea() == null ? "" : report.getArea())
                .replace("{{fechaSolicitud}}", report.getFechaSolicitud() == null ? "" : report.getFechaSolicitud())

                //CREACIÓN DE USUARIOS
                .replace("{{nombresCreacion}}", report.getNombresCreacion() == null ? "" :  report.getNombresCreacion())
                .replace("{{apellidosCreacion}}", report.getApellidosCreacion() == null ? "" : report.getApellidosCreacion())
                .replace("{{nombreUsuarioCreacion}}", report.getNombreUsuarioCreacion() == null ? "" : report.getNombreUsuarioCreacion())
                .replace("{{correoCreacion}}", report.getCorreoCreacion() == null ? "" : report.getCorreoCreacion())
                .replace("{{areaCreacion}}", report.getAreaCreacion() == null ? "" : report.getAreaCreacion())

                //MODIFICACIÓN DE USUARIOS
                .replace("{{usuarioActualModificacion}}", report.getUsuarioActualModificacion() == null ? "" : report.getUsuarioActualModificacion())
                .replace("{{usuarioNuevoModificacion}}", report.getUsuarioNuevoModificacion() == null ? "" : report.getUsuarioNuevoModificacion())
                .replace("{{correoActualModificacion}}", report.getCorreoActualModificacion() == null ? "" : report.getCorreoActualModificacion())
                .replace("{{correoNuevoModificacion}}", report.getCorreoNuevoModificacion() == null ? "" : report.getNombres())

                //DESACTIVACIÓN DE USUARIOS
                .replace("{{nombresCese}}", report.getNombresCese() == null ? "" : report.getNombresCese())
                .replace("{{apellidosCese}}", report.getApellidosCese() == null ? "" : report.getApellidosCese())
                .replace("{{usuarioCese}}", report.getUsuarioCese() == null ? "" : report.getUsuarioCese())
                .replace("{{correoCese}}", report.getCorreoCese() == null ? "" : report.getCorreoCese())
                .replace("{{motivoCese}}", report.getMotivoCese() == null ? "" : report.getMotivoCese())

                // FOOTER
                .replace("{{firmante}}", report.getFirmante() == null ? "" : report.getFirmante());

        return htmlTemplate;
    }

    public String replaceSolicitudEquipoPDFValues(String htmlTemplate, SolicitudEquipoRequest request, String nombreGestor) {
        String nombresApellidos = request.getNombreEmpleado() + ' ' + request.getApellidoPaternoEmpleado() + ' ' + request.getApellidoMaternoEmpleado();
        String checkboxCheckedSymbol = "X";

        String tipoPc = request.getIdTipoEquipo() == 1 ? checkboxCheckedSymbol : "";
        String tipoLaptop = request.getIdTipoEquipo() == 2 ? checkboxCheckedSymbol : "";
        String anexoFijo = request.getIdAnexo() == 1 ? checkboxCheckedSymbol : "";
        String anexoSoftphone = request.getIdAnexo() == 2 ? checkboxCheckedSymbol : "";
        String celularSi = request.getCelular() ? checkboxCheckedSymbol : "";
        String celularNo = !request.getCelular() ? checkboxCheckedSymbol : "";
        String internetSi = request.getInternetMovil() ? checkboxCheckedSymbol : "";
        String internetNo = !request.getInternetMovil() ? checkboxCheckedSymbol : "";

        // lista de productos
        List<String> listaProductos = new ArrayList<>();

        for (SolicitudSoftwareRequest requestSoftware: request.getLstSoftware()) {
            listaProductos.add(Constante.LIST_ITEM
                    .replace("{{numeroItem}}", requestSoftware.getIdItem().toString())
                    .replace("{{producto}}", requestSoftware.getProducto())
                    .replace("{{version}}", requestSoftware.getProdVersion())
            );
        }

        htmlTemplate = htmlTemplate
                .replace("{{title}}", "FT-GS-03 Formulario de Requerimiento de Software y Hardware")
                .replace("{{apellidosNombres}}", nombresApellidos)
                .replace("{{cliente}}", request.getEmpresaCliente())
                .replace("{{area}}", request.getArea())
                .replace("{{cargo}}", request.getPuesto())
                .replace("{{fechaSolicitud}}", Common.parseDateToFormDate(request.getFechaSolicitud()))
                .replace("{{fechaEntrega}}", Common.parseDateToFormDate(request.getFechaEntrega()))
                .replace("{{symbolPc}}", tipoPc)
                .replace("{{symbolLaptop}}", tipoLaptop)
                .replace("{{procesador}}", request.getProcesador())
                .replace("{{ram}}", request.getRam())
                .replace("{{hd}}", request.getHd())
                .replace("{{marca}}", request.getMarca())
                .replace("{{symbolFijo}}", anexoFijo)
                .replace("{{symbolSoftphone}}", anexoSoftphone)
                .replace("{{symbolCelSi}}", celularSi)
                .replace("{{symbolCelNo}}", celularNo)
                .replace("{{symbolIntSi}}", internetSi)
                .replace("{{symbolIntNo}}", internetNo)
                .replace("{{accesorios}}", request.getAccesorios())
                .replace("{{listaProducto}}", String.join("\n", listaProductos))
                .replace("{{nombreFirma}}", nombreGestor)
                .replace("{{nombreGestor}}", nombreGestor);

        return htmlTemplate;
    }
}
