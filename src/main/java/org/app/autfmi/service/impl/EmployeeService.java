package org.app.autfmi.service.impl;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.FilePDFDTO;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.report.SolicitudData;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
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
        String funcionalidades = String.join(",", Constante.GUARDAR_USUARIO, Constante.REALIZAR_INGRESO);
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
            data.setArea("");
            data.setFechaSolicitud(report.getFechaInicioContrato());

            data.setNombresCreacion(report.getNombres());
            data.setApellidosCreacion(report.getApellidos());
            data.setNombreUsuarioCreacion("Sin especificar");
            data.setCorreoCreacion("Sin especificar");
            data.setAreaCreacion("");
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
            data.setNombres(report.getNombres());
            data.setApellidos(report.getApellidos());
            data.setArea("");
            data.setFechaSolicitud(report.getFechaHistorial());
            data.setNombresCese(report.getNombres());
            data.setApellidosCese(report.getApellidos());
            data.setUsuarioCese("No especifica");
            data.setCorreoCese("No especifica");
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
                        (EntryReport) report
                )));

                SolicitudData data = new SolicitudData();
                data.setNombres(((EntryReport) report).getNombres());
                data.setApellidos(((EntryReport) report).getApellidos());
                data.setArea("");
                data.setFechaSolicitud(((EntryReport) report).getFechaInicioContrato());

                data.setNombresCreacion(((EntryReport) report).getNombres());
                data.setApellidosCreacion(((EntryReport) report).getApellidos());
                data.setNombreUsuarioCreacion("Sin especificar");
                data.setCorreoCreacion("Sin especificar");
                data.setAreaCreacion("");
                data.setFirmante(((EntryReport) report).getFirmante());

                solicitudFileB64 = pdfUtils.filePDFToBase64(pdfUtils.crearPDF(pdfUtils.replaceSolicitudPDFValues(
                        pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD), data
                )));

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
                        (MovementReport) report
                )));

                lstfiles.add(new FilePDFDTO("FT-GT-12 Formulario de Movimiento", formularioFileB64));

                // response set up
                response.setBaseResponse(((MovementReport) report).getResponse());
                response.setLstArchivos(lstfiles);
                break;
            }
            case 3: {
                formularioFileB64 = pdfUtils.filePDFToBase64(pdfUtils.crearPDF(pdfUtils.replaceOutRequestValues(
                        pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO),
                        (CeseReport) report
                )));

                SolicitudData data = new SolicitudData();
                data.setNombres(((CeseReport) report).getNombres());
                data.setApellidos(((CeseReport) report).getApellidos());
                data.setArea("");
                data.setFechaSolicitud(((CeseReport) report).getFechaHistorial());
                data.setNombresCese(((CeseReport) report).getNombres());
                data.setApellidosCese(((CeseReport) report).getApellidos());
                data.setUsuarioCese("No especifica");
                data.setCorreoCese("No especifica");
                data.setMotivoCese(((CeseReport) report).getMotivo());
                data.setFirmante(((CeseReport) report).getFirmante());

                solicitudFileB64 = pdfUtils.filePDFToBase64(pdfUtils.crearPDF(pdfUtils.replaceSolicitudPDFValues(
                        pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD), data
                )));

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
