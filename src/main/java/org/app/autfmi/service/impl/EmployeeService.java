package org.app.autfmi.service.impl;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.FilePDFDTO;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.report.*;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.FilePDFResponse;
import org.app.autfmi.model.response.SolicitudEquipoResponse;
import org.app.autfmi.repository.EmployeeRepository;
import org.app.autfmi.repository.HistoryRepository;
import org.app.autfmi.service.IEmployeeService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.app.autfmi.util.PDFUtils;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService implements IEmployeeService {
    private final EmployeeRepository employeeRepository;
    private final HistoryRepository historyRepository;
    private final PDFUtils pdfUtils;
    private final JwtHelper jwt;

    @Override
    public BaseResponse getEmployee(Integer idTalento) {
        return employeeRepository.getEmployee(idTalento);
    }

    @Override
    public BaseResponse saveEmployeeEntry(String token, EmployeeEntryRequest request) throws MessagingException {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = String.join(",", Constante.INSERTAR_TALENTO, Constante.REALIZAR_INGRESO);
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        EntryReport report = historyRepository.registerEntry(baseRequest, request);

        if (report != null && report.getResponse().getIdTipoMensaje() == 2) {
            FileDTO fileFormulario = new FileDTO(
                    "FT-GT-12 Formulario de Ingreso",
                    pdfUtils.replaceEntryRequestValues(pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO),report),
                    null
            );

            SolicitudData data = new SolicitudData();

            data.setNombres(report.getNombres());
            data.setApellidos(report.getApellidos());
            data.setArea(report.getUnidad());
            data.setFechaSolicitud(report.getFechaInicioContrato());

            data.setNombresCreacion(report.getNombres());
            data.setApellidosCreacion(report.getApellidos());
            data.setNombreUsuarioCreacion(report.getUsernameEmpleado());
            data.setCorreoCreacion(report.getEmailEmpleado());
            data.setAreaCreacion(report.getUnidad());
            data.setFirmante(report.getFirmante());

            FileDTO fileSolicitud = new FileDTO(
                    "FT-GS-01 Solicitud de Creación de Usuario",
                    pdfUtils.replaceSolicitudPDFValues(pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD), data),
                    null
            );

            List<FileDTO> lstfiles = new ArrayList<>();
            lstfiles.add(fileFormulario);
            lstfiles.add(fileSolicitud);

            pdfUtils.enviarCorreoConPDF(
                    lstfiles,
                    report.getCorreoGestor(),
                    Collections.emptyList(),
                    "Ingreso de empleado",
                    "Formulario de nuevo ingreso de empleado."
            );
        }

        return report.getResponse();
    }

    @Override
    public BaseResponse saveEmployeeMovement(String token, EmployeeMovementRequest request) throws MessagingException {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.REALIZAR_MOVIMIENTO);
        MovementReport report = historyRepository.registerMovement(baseRequest, request);

        if (report != null && report.getResponse().getIdTipoMensaje() == 2) {
            List<FileDTO> lstfiles = new ArrayList<>();
            FileDTO fileFormulario = new FileDTO(
                    "FT-GT-12 Formulario de Movimiento",
                    pdfUtils.replaceMovementRequestValues(pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO), report),
                    null
            );
            lstfiles.add(fileFormulario);

//            FileDTO fileSolicitud = new FileDTO(
//                    "FT-GS-01 Solicitud de Modificación de Usuario",
//                    pdfUtils.replaceMovementRequestValues(pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD), report),
//                    null
//            );
//            lstfiles.add(fileSolicitud);


            pdfUtils.enviarCorreoConPDF(
                    lstfiles,
                    report.getCorreoGestor(),
                    Collections.emptyList(),
                    "Movimiento de empleado",
                    "Formulario de movimiento de empleado."
            );
        }

        return report.getResponse();
    }

    @Override
    public BaseResponse saveEmployeeContractEnd(String token, EmployeeContractEndRequest request) throws MessagingException {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.REALIZAR_CESE);
        CeseReport report = historyRepository.registerContractTermination(baseRequest, request);

        if (report != null && report.getResponse().getIdTipoMensaje() == 2) {
            FileDTO fileFormulario = new FileDTO(
                    "FT-GT-12 Formulario de Cese",
                    pdfUtils.replaceOutRequestValues(pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO), report),
                    null
            );

            SolicitudData data = new SolicitudData();
            data.setNombres(report.getNombres());
            data.setApellidos(report.getApellidos());
            data.setArea(report.getUnidad());
            data.setFechaSolicitud(report.getFechaHistorial());
            data.setNombresCese(report.getNombres());
            data.setApellidosCese(report.getApellidos());
            data.setUsuarioCese(report.getUsernameEmpleado());
            data.setCorreoCese(report.getEmailEmpleado());
            data.setMotivoCese(report.getMotivo());
            data.setFirmante(report.getFirmante());

            FileDTO fileSolicitud = new FileDTO(
                    "FT-GS-01 Solicitud de Desactivación de Usuario",
                    pdfUtils.replaceSolicitudPDFValues(pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD), data),
                    null
            );

            List<FileDTO> lstfiles = new ArrayList<>();
            lstfiles.add(fileFormulario);
            lstfiles.add(fileSolicitud);

            pdfUtils.enviarCorreoConPDF(
                    lstfiles,
                    report.getCorreoGestor(),
                    Collections.emptyList(),
                    "Cese de empleado",
                    "Formulario de cese del empleado."
            );
        }

        return report.getResponse();
    }

    @Override
    public BaseResponse solicitudEquipo(String token, SolicitudEquipoRequest request) throws MessagingException, SQLServerException {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.REALIZAR_MOVIMIENTO);
        SolicitudEquipoResponse solicitudEquipoResponse = employeeRepository.solicitudEquipo(baseRequest, request);

        if (solicitudEquipoResponse != null && solicitudEquipoResponse.getBaseResponse().getIdTipoMensaje() == 2) {
            List<FileDTO> lstfiles = new ArrayList<>();
            String template = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD_EQUIPO);

            // map request to report
            SolicitudEquipoReport report = mapToSolicitudEquipoReport(request, solicitudEquipoResponse);

            FileDTO fileFormulario = new FileDTO(
                    "FT-GS-03 Formulario de Requerimiento de Software y Hardware",
                    pdfUtils.replaceSolicitudEquipoPDFValues(template, report),
                    null
            );

            lstfiles.add(fileFormulario);

            pdfUtils.enviarCorreoConPDF(
                    lstfiles,
                    solicitudEquipoResponse.getCorreoGestor(),
                    Collections.emptyList(),
                    "Requerimiento de Software y Hardware",
                    "Formulario Requerimiento de Software y Hardware."
            );
        }

        return solicitudEquipoResponse.getBaseResponse();
    }

    private static SolicitudEquipoReport mapToSolicitudEquipoReport(SolicitudEquipoRequest request, SolicitudEquipoResponse solicitudEquipoResponse) {
        SolicitudEquipoReport report = new SolicitudEquipoReport();
        // general
        report.setBaseResponse(solicitudEquipoResponse.getBaseResponse());
        // datos gestor
        report.setCorreoGestor(solicitudEquipoResponse.getCorreoGestor());
        report.setNombreApellidoGestor(solicitudEquipoResponse.getNombres() + ' ' + solicitudEquipoResponse.getApellidos());
        // datos reporte
        report.setNombreEmpleado(request.getNombreEmpleado());
        report.setApellidosEmpleado(request.getApellidoPaternoEmpleado() + ' ' + request.getApellidoMaternoEmpleado());
        report.setCliente(request.getCliente());
        report.setArea(request.getArea());
        report.setPuesto(request.getPuesto());
        report.setFechaSolicitud(request.getFechaSolicitud());
        report.setFechaEntrega(request.getFechaEntrega());
        report.setIdTipoEquipo(request.getIdTipoEquipo());
        report.setProcesador(request.getProcesador());
        report.setRam(request.getRam());
        report.setHd(request.getHd());
        report.setMarca(request.getMarca());
        report.setIdAnexo(request.getIdAnexo());
        report.setCelular(request.getCelular());
        report.setInternetMovil(request.getInternetMovil());
        report.setAccesorios(request.getAccesorios());
        report.setLstSoftware(request.getLstSoftware());
        return report;
    }

    @Override
    public FilePDFResponse getLastHistory(String token, Integer idTipoHistorial, Integer idTalento) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.OBTENER_ULTIMO_REGISTRO_HISTORIAL);
        IReport report = historyRepository.getLastEmployeeHistoryRegister(baseRequest, idTipoHistorial, idTalento);

        FilePDFResponse response = new FilePDFResponse();
        List<FilePDFDTO> lstfiles = new ArrayList<>();

        if (report instanceof EntryReport entry) {
            String formularioFileB64 = pdfUtils.filePDFToBase64(
                    pdfUtils.crearPDF(
                            pdfUtils.replaceEntryRequestValues(
                                    pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO), entry),
                            "FT-GT-12 Formulario de Ingreso"
                    )
            );

            SolicitudData data = new SolicitudData();
            data.setNombres(entry.getNombres());
            data.setApellidos(entry.getApellidos());
            data.setArea(entry.getUnidad());
            data.setFechaSolicitud(entry.getFechaHistorial());
            data.setNombresCreacion(entry.getNombres());
            data.setApellidosCreacion(entry.getApellidos());
            data.setNombreUsuarioCreacion(entry.getUsernameEmpleado());
            data.setCorreoCreacion(entry.getEmailEmpleado());
            data.setAreaCreacion(entry.getUnidad());
            data.setFirmante(entry.getFirmante());

            String solicitudFileB64 = pdfUtils.filePDFToBase64(
                    pdfUtils.crearPDF(
                            pdfUtils.replaceSolicitudPDFValues(
                                    pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD), data),
                            "FT-GS-01 Solicitud de Creación de Usuario"
                    )
            );

            lstfiles.add(new FilePDFDTO("FT-GT-12 Formulario de Ingreso", formularioFileB64));
            lstfiles.add(new FilePDFDTO("FT-GS-01 Solicitud de Creación de Usuario", solicitudFileB64));
            response.setBaseResponse(entry.getResponse());

        } else if (report instanceof MovementReport movement) {
            String formularioFileB64 = pdfUtils.filePDFToBase64(
                    pdfUtils.crearPDF(
                            pdfUtils.replaceMovementRequestValues(
                                    pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO), movement),
                            "FT-GT-12 Formulario de Movimiento"
                    )
            );

            lstfiles.add(new FilePDFDTO("FT-GT-12 Formulario de Movimiento", formularioFileB64));
            response.setBaseResponse(movement.getResponse());

        } else if (report instanceof CeseReport cese) {
            String formularioFileB64 = pdfUtils.filePDFToBase64(
                    pdfUtils.crearPDF(
                            pdfUtils.replaceOutRequestValues(
                                    pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO), cese),
                            "FT-GT-12 Formulario de Cese"
                    )
            );

            SolicitudData data = new SolicitudData();
            data.setNombres(cese.getNombres());
            data.setApellidos(cese.getApellidos());
            data.setArea(cese.getUnidad());
            data.setFechaSolicitud(cese.getFechaHistorial());
            data.setNombresCese(cese.getNombres());
            data.setApellidosCese(cese.getApellidos());
            data.setUsuarioCese(cese.getUsernameEmpleado());
            data.setCorreoCese(cese.getEmailEmpleado());
            data.setMotivoCese(cese.getMotivo());
            data.setFirmante(cese.getFirmante());

            String solicitudFileB64 = pdfUtils.filePDFToBase64(
                    pdfUtils.crearPDF(
                            pdfUtils.replaceSolicitudPDFValues(
                                    pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD), data),
                            "FT-GS-01 Solicitud de Desactivación de Usuario"
                    )
            );

            lstfiles.add(new FilePDFDTO("FT-GT-12 Formulario de Cese", formularioFileB64));
            lstfiles.add(new FilePDFDTO("FT-GS-01 Solicitud de Desactivación de Usuario", solicitudFileB64));
            response.setBaseResponse(cese.getResponse());

        } else if (report instanceof BaseReport baseReport) {
            response.setBaseResponse(baseReport.getResponse());
        }

        response.setLstArchivos(lstfiles);
        return response;
    }

    @Override
    public FilePDFResponse getLastSolicitudEquipo(String token, Integer idSolicitudEquipo) throws MessagingException {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.OBTENER_ULTIMO_REGISTRO_HISTORIAL);
        SolicitudEquipoReport report = historyRepository.getLastSolicitudEquipo(baseRequest, idSolicitudEquipo);
        
        FilePDFResponse response = new FilePDFResponse();
        List<FilePDFDTO> lstfiles = new ArrayList<>();

        if (report != null && report.getBaseResponse().getIdTipoMensaje() == 2) {
            response.setBaseResponse(report.getBaseResponse());
            String template = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD_EQUIPO);
            String fileName = "FT-GS-03 Formulario de Requerimiento de Software y Hardware";

            String solicitudFileB64 = pdfUtils.filePDFToBase64(
                    pdfUtils.crearPDF( pdfUtils.replaceSolicitudEquipoPDFValues(template, report), fileName )
            );

            lstfiles.add(new FilePDFDTO(fileName, solicitudFileB64));

            response.setLstArchivos(lstfiles);
        } else {
            BaseResponse baseResponse = new BaseResponse(3, "Error al obtener solicitud");
            response.setBaseResponse(baseResponse);
            response.setLstArchivos(Collections.emptyList());
        }

        return response;
    }
}
