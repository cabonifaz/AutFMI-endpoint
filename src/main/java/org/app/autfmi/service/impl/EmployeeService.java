package org.app.autfmi.service.impl;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;

import org.app.autfmi.model.builders.ReportPDFBuilder;
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
                    new ArrayList<>(),
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

        if (idTipoHistorial == null)
            throw new IllegalArgumentException("El tipo de historial no puede ser nulo");

        if (idTalento == null)
            throw new IllegalArgumentException("El ID del talento no puede ser nulo");

        IReport report = historyRepository.getHistoryReport(baseRequest, idTalento,
                idTipoHistorial, null, true);
        BaseResponse bs = report.getResponse();
        FilePDFResponse response = new FilePDFResponse();
        response.setBaseResponse(bs);

        if (bs.getIdTipoMensaje() != 2)
            return response;

        PDFUtils pdfUtils = new PDFUtils();
        ReportPDFBuilder builder = new ReportPDFBuilder(pdfUtils);

        if (report instanceof EntryReport entry) {
            // @Pendiente
            GestorDTO gs = new GestorDTO(null, entry.getFirmante());

            List<FileDTO> files = builder
                    .forIngreso(entry, gs)
                    .withFormulario()
                    .withUsuarioInfo()
                    .build();

            List<FilePDFDTO> files64 = files.stream().map(f -> {
                String base64 = pdfUtils.filePDFToBase64(f.byteArchivo);
                return new FilePDFDTO(f.nombreArchivo, base64);
            }).toList();

            response.setLstArchivos(files64);
            return response;

        } else if (report instanceof MovementReport movement) {
            String fullname = movement.getFirmante();
            GestorDTO gs = new GestorDTO(fullname, fullname);
            List<FileDTO> files = builder.forMovimiento(movement, gs)
                    .withFormulario()
                    .build();

            List<FilePDFDTO> files64 = files.stream().map(p -> {
                String base64 = pdfUtils.filePDFToBase64(p.byteArchivo);
                return new FilePDFDTO(p.nombreArchivo, base64);
            }).toList();

            response.setLstArchivos(files64);
            return response;

        } else if (report instanceof CeseReport cese) {
            GestorDTO gs = new GestorDTO(null, cese.getFirmante());

            List<FileDTO> files = builder.forCese(cese, gs)
                    .withFormulario()
                    .withDeactivateRequest()
                    .build();

            List<FilePDFDTO> files64 = files.stream().map(f -> {
                String base64 = pdfUtils.filePDFToBase64(f.byteArchivo);
                return new FilePDFDTO(f.nombreArchivo, base64);
            }).toList();

            response.setLstArchivos(files64);
            return response;

        } else if (report instanceof BaseReport baseReport) {
            response.setBaseResponse(baseReport.getResponse());
        }
        response.setLstArchivos(new ArrayList<>());
        return response;
    }

    @Override
    public FilePDFResponse getLastSolicitudEquipo(String token, Integer talentId)
            throws MessagingException {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.OBTENER_ULTIMO_REGISTRO_HISTORIAL);

        FilePDFResponse response = new FilePDFResponse();

        if (talentId == null)
            throw new IllegalArgumentException("EL ID del talento no puede ser nulo");

        this.logger.info("Fetching last EquipoSolicitud para talento: {}", talentId);
        SolicitudEquipoReport report = historyRepository
                .getSolicitudEquipoReport(
                        baseRequest,
                        talentId,
                        null, true);

        this.logger.info("Report: {}", report);

        BaseResponse rs = report.getBaseResponse();
        response.setBaseResponse(rs);

        if (rs.getIdTipoMensaje() != 2)
            return response;

        // Mapear la respuesta
        PDFUtils pdfUtils = new PDFUtils();
        String gestor = report.getNombreApellidoGestor();
        GestorDTO gs = new GestorDTO(gestor, gestor);
        List<FileDTO> files = new ReportPDFBuilder(pdfUtils)
                .fEquipoReport(report, gs)
                .withFormulario()
                .build();

        List<FilePDFDTO> file64 = files.stream().map(p -> {
            String base64 = pdfUtils.filePDFToBase64(p.byteArchivo);
            return new FilePDFDTO(p.nombreArchivo, base64);
        }).toList();

        this.logger.info("Files in base64: {}", file64.size());

        response.setLstArchivos(file64);

        return response;
    }

    @Override
    public BaseResponse findAllEmployees(String token, Integer page, String searchTerm) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_TALENTOS);
        return this.employeeRepository.findAllEmployees(baseRequest, page, searchTerm);
    }
}
