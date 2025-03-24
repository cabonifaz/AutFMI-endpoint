package org.app.autfmi.service.impl;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.bcel.Const;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.FilePDFDTO;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.report.*;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.FilePDFResponse;
import org.app.autfmi.repository.EmployeeRepository;
import org.app.autfmi.repository.HistoryRepository;
import org.app.autfmi.service.IEmployeeService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.app.autfmi.util.PDFUtils;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService implements IEmployeeService {
    private final EmployeeRepository employeeRepository;
    private final HistoryRepository historyRepository;
    private final PDFUtils pdfUtils;
    private final JwtHelper jwt;

    @Override
    public BaseResponse getEmployee(Integer idUsuarioTalento) {
        return employeeRepository.getEmployee(idUsuarioTalento);
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

            String usernameNuevoUsuario = request.getNombres().charAt(0) + request.getApellidoPaterno().trim().toLowerCase();
            String correoNuevoUsuario = usernameNuevoUsuario + Constante.DOMINIO_CORREO;

            data.setNombres(report.getNombres());
            data.setApellidos(report.getApellidos());
            data.setArea(report.getUnidad());
            data.setFechaSolicitud(report.getFechaInicioContrato());

            data.setNombresCreacion(report.getNombres());
            data.setApellidosCreacion(report.getApellidos());
            data.setNombreUsuarioCreacion(usernameNuevoUsuario);
            data.setCorreoCreacion(correoNuevoUsuario);
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
                    "Ingreso empleado",
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
                    "Ingreso empleado",
                    "Formulario de nuevo ingreso de empleado."
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
            String usernameUsuarioCese = request.getNombres().charAt(0) + request.getApellidoPaterno().trim().toLowerCase();
            String correoUsuarioCese = usernameUsuarioCese + Constante.DOMINIO_CORREO;

            data.setNombres(report.getNombres());
            data.setApellidos(report.getApellidos());
            data.setArea(report.getUnidad());
            data.setFechaSolicitud(report.getFechaHistorial());
            data.setNombresCese(report.getNombres());
            data.setApellidosCese(report.getApellidos());
            data.setUsuarioCese(usernameUsuarioCese);
            data.setCorreoCese(correoUsuarioCese);
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
                    "Cese empleado",
                    "Formulario de cese del empleado."
            );
        }

        return report.getResponse();
    }

    @Override
    public BaseResponse solicitudEquipo(String token, SolicitudEquipoRequest request) throws MessagingException, SQLServerException {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.REALIZAR_MOVIMIENTO);
        SolicitudEquipoReport solicitudEquipoReport = employeeRepository.solicitudEquipo(baseRequest, request);

        if (solicitudEquipoReport != null && solicitudEquipoReport.getBaseResponse().getIdTipoMensaje() == 2) {
            List<FileDTO> lstfiles = new ArrayList<>();
            String template = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD_EQUIPO);
            String nombresApellidos = solicitudEquipoReport.getNombres() + ' ' + solicitudEquipoReport.getApellidos();

            FileDTO fileFormulario = new FileDTO(
                    "FT-GS-03 Formulario de Requerimiento de Software y Hardware",
                    pdfUtils.replaceSolicitudEquipoPDFValues(template, request, nombresApellidos),
                    null
            );

            lstfiles.add(fileFormulario);

            pdfUtils.enviarCorreoConPDF(
                    lstfiles,
                    solicitudEquipoReport.getCorreoGestor(),
                    "Requerimiento de Software y Hardware",
                    "Formulario Requerimiento de Software y Hardware."
            );
        }

        return solicitudEquipoReport.getBaseResponse();
    }

    @Override
    public FilePDFResponse getLastHistory(String token, Integer idTipoHistorial, Integer idUsuarioTalento) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.OBTENER_ULTIMO_REGISTRO_HISTORIAL);
        Object report = historyRepository.getLastEmployeeHistoryRegister(baseRequest, idTipoHistorial, idUsuarioTalento);
        String formularioFileB64;
        String solicitudFileB64;
        FilePDFResponse response = new FilePDFResponse();
        List<FilePDFDTO> lstfiles = new ArrayList<>();

        switch (idTipoHistorial) {
            case 1: {
                formularioFileB64 = pdfUtils.filePDFToBase64(pdfUtils.crearPDF(pdfUtils.replaceEntryRequestValues(
                        pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO),
                        (EntryReport) report),
                        "FT-GT-12 Formulario de Ingreso")
                );

                SolicitudData data = new SolicitudData();
                data.setNombres(((EntryReport) report).getNombres());
                data.setApellidos(((EntryReport) report).getApellidos());
                data.setArea(((EntryReport) report).getUnidad());
                data.setFechaSolicitud(((EntryReport) report).getFechaInicioContrato());

                data.setNombresCreacion(((EntryReport) report).getNombres());
                data.setApellidosCreacion(((EntryReport) report).getApellidos());
                data.setNombreUsuarioCreacion(((EntryReport) report).getUsernameEmpleado());
                data.setCorreoCreacion(((EntryReport) report).getUsernameEmpleado() + Constante.DOMINIO_CORREO);
                data.setAreaCreacion(((EntryReport) report).getUnidad());
                data.setFirmante(((EntryReport) report).getFirmante());

                solicitudFileB64 = pdfUtils.filePDFToBase64(pdfUtils.crearPDF(pdfUtils.replaceSolicitudPDFValues(
                        pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD), data),
                        "FT-GS-01 Solicitud de Creación de Usuario")
                );

                lstfiles.add(new FilePDFDTO("FT-GT-12 Formulario de Ingreso", formularioFileB64));
                lstfiles.add(new FilePDFDTO("FT-GS-01 Solicitud de Creación de Usuario", solicitudFileB64));

                // response set up
                response.setBaseResponse(((EntryReport) report).getResponse());
                response.setLstArchivos(lstfiles);
                break;
            }
            case 2: {
                formularioFileB64 = pdfUtils.filePDFToBase64(pdfUtils.crearPDF(pdfUtils.replaceMovementRequestValues(
                        pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO),
                        (MovementReport) report),
                        "FT-GT-12 Formulario de Movimiento")
                );

                lstfiles.add(new FilePDFDTO("FT-GT-12 Formulario de Movimiento", formularioFileB64));

                // response set up
                response.setBaseResponse(((MovementReport) report).getResponse());
                response.setLstArchivos(lstfiles);
                break;
            }
            case 3: {
                formularioFileB64 = pdfUtils.filePDFToBase64(pdfUtils.crearPDF(pdfUtils.replaceOutRequestValues(
                        pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO),
                        (CeseReport) report),
                        "FT-GT-12 Formulario de Cese")
                );

                SolicitudData data = new SolicitudData();
                data.setNombres(((CeseReport) report).getNombres());
                data.setApellidos(((CeseReport) report).getApellidos());
                data.setArea(((CeseReport) report).getUnidad());
                data.setFechaSolicitud(((CeseReport) report).getFechaHistorial());
                data.setNombresCese(((CeseReport) report).getNombres());
                data.setApellidosCese(((CeseReport) report).getApellidos());
                data.setUsuarioCese(((CeseReport) report).getUsernameEmpleado());
                data.setCorreoCese(((CeseReport) report).getUsernameEmpleado() + Constante.DOMINIO_CORREO);
                data.setMotivoCese(((CeseReport) report).getMotivo());
                data.setFirmante(((CeseReport) report).getFirmante());

                solicitudFileB64 = pdfUtils.filePDFToBase64(pdfUtils.crearPDF(pdfUtils.replaceSolicitudPDFValues(
                        pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD), data),
                        "FT-GS-01 Solicitud de Desactivación de Usuario")
                );

                lstfiles.add(new FilePDFDTO("FT-GT-12 Formulario de Cese", formularioFileB64));
                lstfiles.add(new FilePDFDTO("FT-GS-01 Solicitud de Desactivación de Usuario", solicitudFileB64));

                // response set up
                response.setBaseResponse(((CeseReport) report).getResponse());
                response.setLstArchivos(lstfiles);
                break;
            }
            default: {
                response.setBaseResponse(new BaseResponse(2, "El usuario no tiene registros en historial."));
                response.setLstArchivos(lstfiles);
            }
        }
        return response;
    }
}
