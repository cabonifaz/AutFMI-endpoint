package org.app.autfmi.service.impl;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.FilePDFDTO;
import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.report.*;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.FilePDFResponse;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.repository.EmployeeRepository;
import org.app.autfmi.repository.HistoryRepository;
import org.app.autfmi.service.IEmployeeService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.app.autfmi.util.PDFUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService implements IEmployeeService {

    @Autowired
    private final MailService mailService;
    private final EmployeeRepository employeeRepository;
    private final HistoryRepository historyRepository;
    private final PDFUtils pdfUtils;
    private final JwtHelper jwt;
    private final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    @Override
    public BaseResponse getEmployee(Integer idTalento) {
        return employeeRepository.getEmployee(idTalento);
    }

    @Override
    public BaseResponse saveEmployeeEntry(String token, EmployeeEntryRequest request) throws MessagingException {
        this.logger.info("Processing employee entry");
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = String.join(",", Constante.INSERTAR_TALENTO, Constante.REALIZAR_INGRESO);
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        EntryReport report = historyRepository.registerEntry(baseRequest, request);

        if (report != null && report.getResponse().getIdTipoMensaje() == 2) {
            GestorDTO gs = new GestorDTO(null, report.getFirmante());
            FileDTO fileFormulario = new FileDTO(
                    "FT-GT-12 Formulario de Ingreso",
                    pdfUtils.replaceEntryRequestValues(
                            pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO),
                            report, gs),
                    null);

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
                    pdfUtils.replaceSolicitudPDFValues(
                            pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD),
                            data, null),
                    null);

            List<FileDTO> lstfiles = new ArrayList<>();
            lstfiles.add(fileFormulario);
            lstfiles.add(fileSolicitud);

            pdfUtils.enviarCorreoConPDF(
                    lstfiles,
                    report.getCorreoGestor(),
                    Collections.emptyList(),
                    "Ingreso de empleado",
                    "Formulario de nuevo ingreso de empleado.");
        }

        return report.getResponse();
    }

    /**
     * Saves employee movement and sends notification email with the movement
     * report.
     * 
     * @param token   JWT token for authentication
     * @param request EmployeeMovementRequest containing movement details
     * @return BaseResponse indicating the result of the operation
     */
    @Override
    public BaseResponse saveEmployeeMovement(String token, EmployeeMovementRequest request) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.REALIZAR_MOVIMIENTO);

        OperationResult<Integer> operationResult = historyRepository.registerMovement(baseRequest, request);
        BaseResponse baseResponse = operationResult.getBaseResponse();
        Integer operationId = operationResult.getData();

        if (baseResponse.getIdTipoMensaje() != 2)
            return baseResponse;

        if (baseResponse.getIdTipoMensaje() == 2 && operationId == null)
            return new BaseResponse(2,
                    "Movimiento confirmado, pero no se pudo generar el reporte por falta de ID de movimiento",
                    "No se obtuvo ID de operación");

        // Obtener reporte de movimiento usando el ID de la operación
        this.logger.info("Obteniendo reporte de movimiento para ID de operación: {}", operationId);
        Integer reportType = Constante.TIPO_REPORTE_MOVIMIENTO;
        Integer talentId = request.getIdTalento() == null ? 0 : request.getIdTalento();

        MovementReport report = (MovementReport) historyRepository.getHistoryReport(
                baseRequest,
                talentId,
                reportType,
                operationId,
                false);

        this.mailService.sendMovementReportNotification(report);

        return baseResponse;
    }

    /**
     * Saves employee contract termination and sends notification email with the
     * termination report.
     * 
     * @param token   JWT token for authentication
     * @param request EmployeeContractEndRequest containing termination details
     * @return BaseResponse indicating the result of the operation
     */
    @Override
    public BaseResponse saveEmployeeContractEnd(String token, EmployeeContractEndRequest request) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.REALIZAR_CESE);

        OperationResult<Integer> operationResult = historyRepository.registerContractTermination(baseRequest,
                request);
        BaseResponse response = operationResult.getBaseResponse();
        Integer operationId = operationResult.getData();

        if (response.getIdTipoMensaje() != 2)
            return response;

        if (response.getIdTipoMensaje() == 2 && operationId == null) {
            String msg = "Cese confirmado, pero no se pudo generar el reporte por falta de ID de cese";
            String detail = "No se obtuvo ID de operación";
            this.logger.error("No se pudo generar  el reporte de cese: {} - {}", msg, detail);
            return new BaseResponse(2, msg, detail);
        }

        // Obtener reporte de cese usando el ID de la operación
        this.logger.info("Obteniendo reporte de cese para ID de operación: {}", operationId);
        Integer reportType = Constante.TIPO_REPORTE_CESE;
        Integer talentId = request.getIdTalento() == null ? 0 : request.getIdTalento();
        CeseReport report = (CeseReport) historyRepository.getHistoryReport(baseRequest, talentId, reportType,
                operationId, false);

        this.logger.info("Generando y enviando correo con reporte de cese");
        this.mailService.sendCeseReportNotification(report);
        return response;
    }

    @Override
    public BaseResponse solicitudEquipo(String token, SolicitudEquipoRequest request) throws SQLServerException {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.REALIZAR_MOVIMIENTO);
        OperationResult<Integer> response = employeeRepository.insertSolicitudEquipo(baseRequest,
                request);

        if (response.getBaseResponse().getIdTipoMensaje() != 2)
            return response.getBaseResponse();

        if (response.getData() == null)
            return new BaseResponse(2,
                    "Se completó el registro de la solicitud, pero no se pudo obtener el ID de la solicitud");

        // Obtener detalles de la solicitud para generar el reporte
        Integer talentId = request.getIdTalento() == null ? 0 : request.getIdTalento();
        Integer operationId = response.getData();

        SolicitudEquipoReport rp = historyRepository.getSolicitudEquipoReport(baseRequest,
                talentId, operationId, false);

        // Enviar correo con el reporte generado
        this.mailService.sendEquipmentRequestNotification(rp);

        return response.getBaseResponse();
    }

    @Override
    public FilePDFResponse getLastHistory(String token, Integer idTipoHistorial, Integer idTalento) {
        this.logger.info("Processing getLastHistory");
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.OBTENER_ULTIMO_REGISTRO_HISTORIAL);
        IReport report = historyRepository.getLastEmployeeHistoryRegister(baseRequest, idTipoHistorial,
                idTalento);

        FilePDFResponse response = new FilePDFResponse();
        List<FilePDFDTO> lstfiles = new ArrayList<>();

        if (report instanceof EntryReport entry) {
            GestorDTO gs = new GestorDTO(null, entry.getFirmante());
            String formularioFileB64 = pdfUtils.filePDFToBase64(
                    pdfUtils.crearPDF(
                            pdfUtils.replaceEntryRequestValues(
                                    pdfUtils.getHtmlTemplate(
                                            PDFUtils.TemplateType.FORMULARIO),
                                    entry, gs),
                            "FT-GT-12 Formulario de Ingreso"));

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

            // GestorDTO gs = new GestorDTO(null, data.getFirmante());
            String template = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD);
            String formattedTemplate = pdfUtils.replaceSolicitudPDFValues(
                    template,
                    data, gs);

            byte[] fileBytes = pdfUtils.crearPDF(formattedTemplate,
                    "FT-GS-01 Solicitud de Creación de Usuario");

            String solicitudFileB64 = pdfUtils.filePDFToBase64(fileBytes);

            lstfiles.add(new FilePDFDTO("FT-GT-12 Formulario de Ingreso", formularioFileB64));
            lstfiles.add(new FilePDFDTO("FT-GS-01 Solicitud de Creación de Usuario", solicitudFileB64));
            response.setBaseResponse(entry.getResponse());

        } else if (report instanceof MovementReport movement) {
            String formularioFileB64 = pdfUtils.filePDFToBase64(
                    pdfUtils.crearPDF(
                            pdfUtils.replaceMovementRequestValues(
                                    pdfUtils.getHtmlTemplate(
                                            PDFUtils.TemplateType.FORMULARIO),
                                    movement),
                            "FT-GT-12 Formulario de Movimiento"));

            lstfiles.add(new FilePDFDTO("FT-GT-12 Formulario de Movimiento", formularioFileB64));
            response.setBaseResponse(movement.getResponse());

        } else if (report instanceof CeseReport cese) {
            GestorDTO gs = new GestorDTO(null, cese.getFirmante());
            String formularioFileB64 = pdfUtils.filePDFToBase64(
                    pdfUtils.crearPDF(
                            pdfUtils.replaceOutRequestValues(
                                    pdfUtils.getHtmlTemplate(
                                            PDFUtils.TemplateType.FORMULARIO),
                                    cese, gs),
                            "FT-GT-12 Formulario de Cese"));

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
                                    pdfUtils.getHtmlTemplate(
                                            PDFUtils.TemplateType.SOLICITUD),
                                    data, gs),
                            "FT-GS-01 Solicitud de Desactivación de Usuario"));

            lstfiles.add(new FilePDFDTO("FT-GT-12 Formulario de Cese", formularioFileB64));
            lstfiles.add(new FilePDFDTO("FT-GS-01 Solicitud de Desactivación de Usuario",
                    solicitudFileB64));
            response.setBaseResponse(cese.getResponse());

        } else if (report instanceof BaseReport baseReport) {
            response.setBaseResponse(baseReport.getResponse());
        }

        response.setLstArchivos(lstfiles);
        return response;
    }

    @Override
    public FilePDFResponse getLastSolicitudEquipo(String token, Integer talentId)
            throws MessagingException {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.OBTENER_ULTIMO_REGISTRO_HISTORIAL);
        SolicitudEquipoReport report = historyRepository.getLastSolicitudEquipo(baseRequest, talentId);
        // this.logger.info("Generated report for solicitud equipo: {}", report);

        FilePDFResponse response = new FilePDFResponse();
        List<FilePDFDTO> lstfiles = new ArrayList<>();

        if (report != null && report.getBaseResponse().getIdTipoMensaje() == 2) {
            response.setBaseResponse(report.getBaseResponse());
            String template = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD_EQUIPO);
            String fileName = "FT-GS-03 Formulario de Requerimiento de Software y Hardware";

            GestorDTO gs = new GestorDTO(null,
                    report.getNombreApellidoGestor());
            String solicitudFileB64 = pdfUtils.filePDFToBase64(
                    pdfUtils.crearPDF(
                            pdfUtils.replaceSolicitudEquipoPDFValues(template, report, gs),
                            fileName));

            lstfiles.add(new FilePDFDTO(fileName, solicitudFileB64));

            response.setLstArchivos(lstfiles);
        } else if (report != null && report.getBaseResponse().getIdTipoMensaje() != 3) {
            BaseResponse baseResponse = new BaseResponse(3, report.getBaseResponse().getMensaje());
            response.setBaseResponse(baseResponse);
            response.setLstArchivos(Collections.emptyList());
        } else {
            BaseResponse baseResponse = new BaseResponse(3, "Error al obtener solicitud");
            response.setBaseResponse(baseResponse);
            response.setLstArchivos(Collections.emptyList());
        }

        return response;
    }
}
