package org.app.autfmi.service.impl;

import com.microsoft.sqlserver.jdbc.SQLServerException;

import lombok.RequiredArgsConstructor;

import org.app.autfmi.model.builders.ReportPDFBuilder;
import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.GestorDTO;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.dto.VacanteCarreraDTO;
import org.app.autfmi.model.dto.VacanteSkillDTO;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.RequirementReport;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.FileResponse;
import org.app.autfmi.model.response.VacanteSkillsResponse;
import org.app.autfmi.repository.HistoryRepository;
import org.app.autfmi.repository.RequirementRepository;
import org.app.autfmi.service.IRequirementService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.FileUtils;
import org.app.autfmi.util.JwtHelper;
import org.app.autfmi.util.MailUtils;
import org.app.autfmi.util.PDFUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequirementService implements IRequirementService {

    private final RequirementRepository requirementRepository;
    private final JwtHelper jwt;
    private final Logger logger = LoggerFactory.getLogger(RequirementService.class);
    private final MailUtils mailUtils;
    private final PDFUtils pdfUtils;
    private final ReportPDFBuilder reportPDFBuilder;
    private final HistoryRepository historyRepository;

    @Autowired
    private MailService mailService;

    @Override
    public BaseResponse listRequirements(String token, Integer nPag, Integer cPag, Integer idCliente, String buscar,
            Date fechaSolicitud, Integer estado) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_REQUERIMIENTOS);
        return requirementRepository.listRequirements(baseRequest, nPag, cPag, idCliente, buscar, fechaSolicitud,
                estado);
    }

    @Override
    public BaseResponse getRequirement(String token, Integer idRequerimiento, Boolean showfiles,
            Boolean showVacantesList, Boolean showContactList) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.DETALLE_REQUERIMIENTO);
        return requirementRepository.getRequirementById(idRequerimiento, showfiles, showVacantesList, showContactList,
                baseRequest);
    }

    @Override
    public BaseResponse saveRequirement(String token, RequirementRequest request) {
        try {
            UserDTO user = jwt.decodeToken(token);
            String funcionalidades = Constante.GUARDAR_REQUERIMIENTO;
            BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
            BaseResponse response = requirementRepository.saveRequirement(request, baseRequest);

            if (response.getIdTipoMensaje() != 2) {
                return response;
            }

            // Obtener el reporte del requerimiento guardado
            RequirementReport report = requirementRepository.getRequirementReport(
                    Integer.parseInt(response.getMensaje()), baseRequest.getIdUsuario());

            List<String> toAddresses = new ArrayList<>();

            for (var manager : report.getManagers()) {
                toAddresses.add(manager.getEmail());
            }

            // Agregar el correo del usuario que realizó la acción
            if (report.getActionUser() != null && report.getActionUser().getCorreo() != null) {
                toAddresses.add(report.getActionUser().getCorreo());
            }

            // Notificar también a los correos adicionales si existen
            if (report.getExtraMailList() != null && !report.getExtraMailList().isEmpty()) {
                toAddresses.addAll(report.getExtraMailList());
            }

            String subject = "Detalle Requerimiento " + report.getRequirementDetails().getCodigoRQ();

            // Enviar notificación por correo electrónico utilizando el servicio de correo
            try {
                this.mailService.sendRequirementNotificationV2(
                        report,
                        subject,
                        toAddresses,
                        new ArrayList<>(),
                        "CREAR_REQUERIMIENTO");

            } catch (Exception e) {
                this.logger.error("Error al enviar notificación de requerimiento: {}", e);
            }
            response.setMensaje("RQ creado exitosamente");
            return response;
        } catch (SQLServerException e) {
            this.logger.error("SQLServerException al guardar requerimiento: {}", e);
            return new BaseResponse(3, "Error al guardar requerimiento", e.getMessage());
        } catch (NullPointerException ex) {
            this.logger.error("NullPointerException al guardar requerimiento: {}", ex);
            return new BaseResponse(3, "Error al guardar requerimiento", ex.getMessage());
        } catch (Exception e) {
            this.logger.error("Exception al guardar requerimiento: {}", e);
            return new BaseResponse(3, "Error al guardar requerimiento", e.getMessage());
        }
    }

    /**
     * Método para guardar un requerimiento creado por un agente externo
     * 
     * @param token   Token de autenticación del usuario
     * @param request Datos del requerimiento a guardar
     * @return Respuesta con el resultado de la operación, con el ID-RQ en el
     *         mensaje si es exitoso
     */
    @Override
    public BaseResponse saveRequirementByAgent(String token, AgentRQRequest request) {
        try {

            this.logger.info("Iniciando saveRequirementByAgent para: {}", request.getTitulo());
            this.logger.info("Contacts size: {}", request.getLstContactos().size());
            this.logger.info("Vacantes size: {}", request.getLstVacantes().size());
            this.logger.info("Vacantes details: {}", request.getLstVacantes().toString());

            UserDTO user = jwt.decodeToken(token);
            String funcionalidades = Constante.GUARDAR_REQUERIMIENTO;
            BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
            BaseResponse rs = requirementRepository.saveRequirementByAgent(request, baseRequest);

            if (rs.getIdTipoMensaje() != 2) {
                return rs;
            }

            RequirementReport report = requirementRepository.getRequirementReport(
                    Integer.parseInt(rs.getMensaje()), baseRequest.getIdUsuario());

            List<String> toAddresses = new ArrayList<>();

            for (var manager : report.getManagers()) {
                toAddresses.add(manager.getEmail());
            }

            // Notificar también a los correos adicionales si existen
            if (report.getExtraMailList() != null && !report.getExtraMailList().isEmpty()) {
                toAddresses.addAll(report.getExtraMailList());
            }

            String subject = "Detalle Requerimiento " + report.getRequirementDetails().getCodigoRQ();

            // Enviar notificación por correo electrónico utilizando el servicio de correo
            this.mailService.sendRequirementNotificationV2(
                    report,
                    subject,
                    toAddresses,
                    new ArrayList<>(),
                    "CREAR_EDITAR_REQUERIMIENTO_AGENTE");

            return rs;
        } catch (SQLServerException e) {
            this.logger.error("SQLServerException al guardar requerimiento por agente: {}", e.getMessage());
            this.logger.error("Error: {}", e);
            return new BaseResponse(3, "Error al guardar requerimiento por agente", e.getMessage());
        } catch (Exception e) {
            this.logger.error("Exception al guardar requerimiento por agente: {}", e.getMessage());
            this.logger.error("Error: {}", e);
            return new BaseResponse(3, "Error al guardar requerimiento por agente", e.getMessage());
        }
    }

    @Override
    public BaseResponse updateRequirement(String token, RequirementRequest request) {
        try {
            UserDTO user = jwt.decodeToken(token);
            String funcionalidades = Constante.ACTUALIZAR_REQUERIMIENTO;
            BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
            BaseResponse response = requirementRepository.updateRequirement(request, baseRequest);

            if (response.getIdTipoMensaje() != 2) {
                return response;
            }

            // Obtener el reporte del requerimiento actualizado
            RequirementReport report = requirementRepository.getRequirementReport(
                    request.getIdRequerimiento(), baseRequest.getIdUsuario());

            List<String> toAddresses = new ArrayList<>();
            for (var manager : report.getManagers()) {
                toAddresses.add(manager.getEmail());
            }
            // Agregar el correo del usuario que realizó la acción
            if (report.getActionUser() != null && report.getActionUser().getCorreo() != null) {
                toAddresses.add(report.getActionUser().getCorreo());
            }
            String subject = "Detalle Requerimiento " + report.getRequirementDetails().getCodigoRQ();
            // Enviar notificación por correo electrónico utilizando el servicio de correo
            try {
                this.mailService.sendRequirementNotificationV2(
                        report,
                        subject,
                        toAddresses,
                        new ArrayList<>(),
                        "ACTUALIZAR_REQUERIMIENTO");
            } catch (Exception e) {
                this.logger.error("Error al enviar notificación de actualización de requerimiento: {}", e);
            }
            response.setMensaje("RQ actualizado exitosamente");
            return response;

        } catch (SQLServerException e) {
            this.logger.error("SQLServerException al actualizar requerimiento: {}", e);
            return new BaseResponse(3, "No se pudo actualizar el requerimiento", e.getMessage());
        } catch (Exception e) {
            this.logger.error("Exception al actualizar requerimiento: {}", e);
            return new BaseResponse(3, "No se pudo actualizar el requerimiento", e.getMessage());
        }
    }

    @Override
    public BaseResponse saveRequirementTalents(String token, RequirementTalentRequest request)
            throws SQLServerException {

        var user = jwt.decodeToken(token);
        var funcionalidades = Constante.GUARDAR_REQUERIMIENTO;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        var response = requirementRepository.saveRequirementTalents(request, baseRequest);
        var baseResponse = response.getBaseResponse();

        // Validación de respuesta
        if (baseResponse.getIdTipoMensaje() != 2)
            return baseResponse;

        this.logger.info("Talentos confirmados guardados");

        if (!request.getFlagCorreo())
            return baseResponse;

        // Envío de notificaciones
        try {
            this.logger.info("Enviando notificaciones");

            var gestorRq = response.getGestorRq();
            var gestorRqEmail = gestorRq.getCorreo();
            var ccList = response.getCcList();
            var postulantes = response.getPostulantes();

            if (gestorRqEmail == null) {
                this.logger.error("No se encontro el correo del gestor del requerimiento");
                return baseResponse;
            }

            if (ccList == null) {
                this.logger.warn("No se encontro la lista de correo de los cc");
                ccList = Collections.emptyList();
            }

            /* Notificar sobre talentos confirmados */
            if (postulantes != null && !postulantes.isEmpty()) {
                this.mailUtils.sendRequirementPostulantMail(
                        gestorRq,
                        "Ingreso de nuevo talento",
                        postulantes,
                        ccList);
                this.logger.info("Notificación de talentos confirmados enviada");
            }

            /* Formularos de ingresos IDs */
            this.logger.info("Generando formularos de ingresos");
            var entryReportsIds = response.getReportesIngreso();

            if (entryReportsIds != null) {
                List<FileDTO> filesToSend = new ArrayList<>();
                entryReportsIds.stream().forEach((report) -> {

                    // Obtener reporte de ingreso
                    baseRequest.setFuncionalidades(Constante.OBTENER_ULTIMO_REGISTRO_HISTORIAL);

                    var idTalento = report.getIdTalento();

                    if (idTalento == null) {
                        this.logger.error("No se encontro el talento para el reporte de ingreso");
                        return;
                    }

                    var entryReport = (EntryReport) this.historyRepository.getHistoryReport(
                            baseRequest,
                            idTalento,
                            report.getIdTipoHistorial(),
                            report.getIdHistorial(),
                            false);

                    // TODO: Remover gestor del ReportBuilder, ya no es necesario
                    var gs = new GestorDTO("", "");
                    var files = this.reportPDFBuilder
                            .forIngreso(entryReport, gs)
                            .withFormulario()
                            .withCreateUser()
                            .build();

                    filesToSend.addAll(files);
                });

                this.logger.info("Formularios de ingresos generados: {}", filesToSend.size());

                if (!filesToSend.isEmpty()) {

                    this.logger.info("Enviando formularos de ingresos");
                    pdfUtils.enviarCorreoConPDF(
                            filesToSend,
                            gestorRqEmail,
                            ccList,
                            "Ingreso de empleado",
                            "Formulario de nuevo ingreso de empleado.");
                    filesToSend.clear();
                    this.logger.info("Formularos de ingresos enviados");
                }
            }

            /** Notificar sobre solicitudes de equipo */
            var solicitudesEqipo = response.getReportesSolicitudEquipo();
            if (solicitudesEqipo != null && !solicitudesEqipo.isEmpty()) {
                List<FileDTO> filesToSend = new ArrayList<>();
                // Obtener las solicitudes de equipo
                this.logger.info("Generando formularos de solicitudes de equipo");
                response.getReportesSolicitudEquipo().stream().forEach((solicitud) -> {

                    var reporte = this.historyRepository.getSolicitudEquipoReport(
                            baseRequest,
                            solicitud.getIdTalento(),
                            solicitud.getIdSolicitudEquipo(),
                            false);

                    // TODO: Remover gestor del ReportBuilder, ya no es necesario
                    var gs = new GestorDTO("", "");

                    var files = this.reportPDFBuilder
                            .fEquipoReport(reporte, gs)
                            .withFormulario()
                            .build();
                    filesToSend.addAll(files);
                });

                this.logger.info("Formularos de solicitudes de equipo generados: {}", filesToSend.size());

                if (!filesToSend.isEmpty()) {
                    this.logger.info("Enviando formularos de solicitudes de equipo");
                    pdfUtils.enviarCorreoConPDF(
                            filesToSend,
                            gestorRqEmail,
                            ccList,
                            "Solicitud de equipo",
                            "Formulario de solicitud de equipo.");
                    filesToSend.clear();
                    this.logger.info("Formularos de solicitudes de equipo enviados");
                }
            }

        } catch (Exception e) {
            this.logger.error("Error al enviar notificaciones: {}", e);
        }

        return response.getBaseResponse();

    }

    @Override
    public BaseResponse getRequirementTalentData(String token, Integer idTalento, Integer idRequerimiento) {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = Constante.MOSTRAR_DATOS_TALENTO;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.getRequirementTalentData(baseRequest, idTalento, idRequerimiento);
    }

    @Override
    public BaseResponse saveRequirementFile(String token, RequirementFileRequest request) throws SQLServerException {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = Constante.GUARDAR_ARCHIVOS;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.saveRequirementFile(baseRequest, request);
    }

    @Override
    public BaseResponse removeRequirementFile(String token, Integer idRqFile) {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = Constante.ELIMINAR_ARCHIVOS;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.removeRequirementFile(baseRequest, idRqFile);
    }

    @Override
    public FileResponse getRequirementFile(String token, Integer idrqFile) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_REQUERIMIENTOS);
        FileResponse fileResponse = requirementRepository.getRqFile(baseRequest, idrqFile);
        String fileBase64 = "";
        if (fileResponse.getBaseResponse().getIdTipoMensaje() == 2 && fileResponse != null) {
            fileBase64 = FileUtils.cargarArchivoAws(fileResponse.getFile());
            fileResponse.setFile(fileBase64);
        }

        if (fileBase64.isEmpty()) {
            fileResponse.setBaseResponse(new BaseResponse(1, "Archivo no encontrado"));
        }

        return fileResponse;
    }

    @Override
    public VacanteSkillsResponse getTechSkillsForVac(String token, Integer idVacante) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = new BaseRequest();
        baseRequest.setUsername(user.getUsuario());
        baseRequest.setIdRol(user.getIdRoles().get(0));
        baseRequest.setIdUsuario(user.getIdUsuario());
        return requirementRepository.getTechSkillsForVac(idVacante);
    }

    @Override
    public BaseResponse updateSkillsForVac(String token, Integer idVacante,
            List<VacanteSkillDTO> skills) {

        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, "");
        return requirementRepository.updateSkillsForVac(baseRequest, idVacante, skills);

    }

    @Override
    public BaseResponse updateCareersForVac(String token, Integer idVacante, List<VacanteCarreraDTO> careers) {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = Constante.ACTUALIZAR_REQUERIMIENTO;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.updateCareersForVac(idVacante, baseRequest, careers);
    }

    @Override
    public BaseResponse getCareersForVac(String token, Integer idVacante) {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = Constante.LISTAR_REQUERIMIENTOS;
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        return requirementRepository.getCareersForVac(baseRequest, idVacante);
    }

}
