package org.app.autfmi.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.dto.VacanteCarreraDTO;
import org.app.autfmi.model.dto.VacanteSkillDTO;
import org.app.autfmi.model.report.RequirementReport;
import org.app.autfmi.model.request.AgentRQRequest;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.FileRequest;
import org.app.autfmi.model.request.RequirementFileRequest;
import org.app.autfmi.model.request.RequirementRequest;
import org.app.autfmi.model.request.RequirementTalentRequest;
import org.app.autfmi.model.request.RqFileConfirmRequest;
import org.app.autfmi.model.request.RqFileDownloadRequest;
import org.app.autfmi.model.request.RqFileUploadUrlRequest;
import org.app.autfmi.model.request.RtFileConfirmRequest;
import org.app.autfmi.model.request.RtFileDownloadRequest;
import org.app.autfmi.model.request.RtFileUploadUrlRequest;
import org.app.autfmi.model.dto.PostulantFileDTO;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.FileResponse;
import org.app.autfmi.model.response.PostulantFileListResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import org.app.autfmi.model.response.RqFileUploadUrlDTO;
import org.app.autfmi.model.response.RqPresignedUrlResponse;
import org.app.autfmi.model.response.SaveRequirementResponse;
import org.app.autfmi.model.response.VacanteSkillsResponse;
import org.app.autfmi.repository.RequirementRepository;
import org.app.autfmi.service.IMailService;
import org.app.autfmi.service.IRequirementService;
import org.app.autfmi.util.ClientS3V2;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.FileUtils;
import org.app.autfmi.util.JwtHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.sqlserver.jdbc.SQLServerException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequirementService implements IRequirementService {

    private final RequirementRepository requirementRepository;
    private final JwtHelper jwt;
    private final Logger logger = LoggerFactory.getLogger(RequirementService.class);
    private final NotificationService notificationService;
    private final IMailService mailService;
    private final ClientS3V2 clientS3;

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

            Integer idRequerimiento = Integer.parseInt(response.getMensaje());

            // Obtener el reporte del requerimiento guardado
            RequirementReport report = requirementRepository.getRequirementReport(
                    idRequerimiento, baseRequest.getIdUsuario());

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
            // Generar una URL PUT pre-firmada por cada archivo. El front subirá cada
            // archivo (que mantiene en memoria) directamente a S3 en el mismo orden.
            List<RqFileUploadUrlDTO> archivos = new ArrayList<>();
            if (request.getLstArchivos() != null) {
                for (FileRequest file : request.getLstArchivos()) {
                    String fileName = file.getNombreArchivo() + "." + file.getExtensionArchivo();
                    String path = buildRqFilePath(baseRequest.getIdEmpresa(), idRequerimiento, fileName);
                    String url = clientS3.generatePresignedUploadUrl(path, file.getContentType(), 5);
                    archivos.add(new RqFileUploadUrlDTO(url, path, fileName, file.getIdTipoArchivoRQ()));
                }
            }

            SaveRequirementResponse saveResponse = new SaveRequirementResponse();
            saveResponse.setIdTipoMensaje(2);
            saveResponse.setMensaje("RQ creado exitosamente");
            saveResponse.setIdRequerimiento(idRequerimiento);
            saveResponse.setArchivos(archivos);
            return saveResponse;
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

    // -------------------------------- Envio de correos al guardar talentos confirmados --------------------------------

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

        // Envío de notificaciones de forma asíncrona
        try {
            this.logger.info("Delegando envío de notificaciones al servicio asíncrono");

            var gestorRq = response.getGestorRq();
            var ccList = response.getCcList();
            var postulantes = response.getPostulantes();
            var entryReportsIds = response.getReportesIngreso();
            var solicitudesEquipo = response.getReportesSolicitudEquipo();

            // Validación básica antes de delegar
            if (gestorRq == null || gestorRq.getCorreo() == null) {
                this.logger.error("No se encontró el correo del gestor del requerimiento");
                // El guardado sí ocurrió (idTipoMensaje == 2), pero la notificación no pudo
                // enviarse. Lo señalamos en detalleMensaje sin degradar el estado de éxito,
                // para que el frontend pueda advertirlo.
                baseResponse.setDetalleMensaje(
                        "No se pudo notificar por correo al usuario que ejecutó la acción: correo no disponible");
                return baseResponse;
            }

            // Crear una copia del baseRequest para el contexto asíncrono
            var asyncBaseRequest = Common.createBaseRequest(
                    user,
                    baseRequest.getFuncionalidades());

            // Llamada asíncrona
            notificationService.sendRequirementNotifications(
                gestorRq,
                ccList != null ? ccList : Collections.emptyList(),
                postulantes,
                entryReportsIds,
                solicitudesEquipo,
                asyncBaseRequest
            );

            this.logger.info("Notificaciones delegadas al servicio asíncrono");

        } catch (Exception e) {
            this.logger.error("Error al delegar notificaciones: {}", e.getMessage(), e);
        }

        return response.getBaseResponse();
    }

    // ---------------------------------------------------------------------------------------------------------

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

    // ─── Archivos por URL pre-firmada (detalle/actualización de RQ) ─────────────

    @Override
    public RqPresignedUrlResponse generateRqUploadUrl(String token, RqFileUploadUrlRequest request) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.GUARDAR_ARCHIVOS);

        if (request.getIdRequerimiento() == null) {
            return new RqPresignedUrlResponse(new BaseResponse(3, "Requerimiento inválido"), null, null, null);
        }
        if (request.getFileName() == null || request.getFileName().trim().isEmpty()) {
            return new RqPresignedUrlResponse(new BaseResponse(3, "Nombre de archivo inválido"), null, null, null);
        }

        String originalFilename = request.getFileName();
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        String cleanName = originalFilename;
        if (cleanName.length() > 100) {
            cleanName = cleanName.substring(0, 95) + extension;
        }

        // Nombre único en S3 para no sobrescribir archivos con el mismo nombre.
        String generatedFileName = System.currentTimeMillis() + "_" + cleanName.replaceAll("\\s+", "_");
        String path = buildRqFilePath(baseRequest.getIdEmpresa(), request.getIdRequerimiento(), generatedFileName);

        String url = clientS3.generatePresignedUploadUrl(path, request.getContentType(), 5);
        if (url == null || url.isEmpty()) {
            return new RqPresignedUrlResponse(new BaseResponse(3, "Error generando URL"), null, null, null);
        }

        return new RqPresignedUrlResponse(
                new BaseResponse(2, "URL generada correctamente"), url, path, cleanName);
    }

    @Override
    public BaseResponse confirmRqUpload(String token, RqFileConfirmRequest request) throws SQLServerException {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.GUARDAR_ARCHIVOS);

        if (request.getPath() == null || request.getPath().trim().isEmpty()) {
            return new BaseResponse(3, "Ruta de archivo inválida");
        }

        // Verificar que el archivo exista físicamente en S3 antes de registrarlo.
        if (!clientS3.exists(request.getPath())) {
            return new BaseResponse(3, "El archivo no existe en S3");
        }

        return requirementRepository.confirmRequirementFile(baseRequest, request);
    }

    @Override
    public RqPresignedUrlResponse generateRqDownloadUrl(String token, RqFileDownloadRequest request) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_REQUERIMIENTOS);

        // getRqFile devuelve la ruta S3 (LINK) en el campo 'file'.
        FileResponse fileResponse = requirementRepository.getRqFile(baseRequest, request.getIdArchivo());

        if (fileResponse == null || fileResponse.getBaseResponse() == null
                || fileResponse.getBaseResponse().getIdTipoMensaje() != 2
                || fileResponse.getFile() == null || fileResponse.getFile().isEmpty()) {
            return new RqPresignedUrlResponse(new BaseResponse(3, "Archivo no encontrado"), null, null, null);
        }

        String path = fileResponse.getFile();
        String url = clientS3.generatePresignedUrl(path, 5);
        if (url == null || url.isEmpty()) {
            return new RqPresignedUrlResponse(new BaseResponse(3, "Error generando URL de descarga"), null, null, null);
        }

        String fileName = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
        return new RqPresignedUrlResponse(new BaseResponse(2, "URL generada correctamente"), url, null, fileName);
    }

    /**
     * Construye la ruta (key) S3 de un archivo de requerimiento, idéntica a la que
     * el SP almacena en la columna LINK: repositorio/{idEmpresa}/{idRq}/archivos/{archivo}.
     */
    private String buildRqFilePath(Integer idEmpresa, Integer idRequerimiento, String fileName) {
        return Constante.RUTA_REPOSITORIO + idEmpresa
                + Constante.RUTA_RQ_ARCHIVOS.replace("[ID_REQUERIMIENTO]", idRequerimiento.toString())
                + fileName;
    }

    // ─── Archivos de postulante (REQUERIMIENTO_TALENTO) por URL pre-firmada ──────

    public RqPresignedUrlResponse generatePostulantUploadUrl(String token, RtFileUploadUrlRequest request) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.GUARDAR_ARCHIVOS);

        if (request.getIdRequerimientoTalento() == null) {
            return new RqPresignedUrlResponse(new BaseResponse(3, "Postulante inválido"), null, null, null);
        }
        if (request.getFileName() == null || request.getFileName().trim().isEmpty()) {
            return new RqPresignedUrlResponse(new BaseResponse(3, "Nombre de archivo inválido"), null, null, null);
        }

        // Extensión permitida (whitelist).
        String extension = extractExtension(request.getFileName());
        if (!Constante.EXT_ARCHIVO_POSTULANTE.contains(extension)) {
            return new RqPresignedUrlResponse(new BaseResponse(3, "Tipo de archivo no permitido"), null, null, null);
        }

        // Nombre saneado (solo caracteres seguros) y único en S3.
        String cleanName = sanitizeFileName(request.getFileName(), extension);
        String generatedFileName = System.currentTimeMillis() + "_" + cleanName;
        String path = buildPostulantFilePath(baseRequest.getIdEmpresa(), request.getIdRequerimientoTalento(),
                generatedFileName);

        String url = clientS3.generatePresignedUploadUrl(path, request.getContentType(), 5);
        if (url == null || url.isEmpty()) {
            return new RqPresignedUrlResponse(new BaseResponse(3, "Error generando URL"), null, null, null);
        }

        return new RqPresignedUrlResponse(
                new BaseResponse(2, "URL generada correctamente"), url, path, cleanName);
    }

    public BaseResponse confirmPostulantUpload(String token, RtFileConfirmRequest request) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.GUARDAR_ARCHIVOS);

        if (request.getPath() == null || request.getPath().trim().isEmpty()) {
            return new BaseResponse(3, "Ruta de archivo inválida");
        }
        // El tipo de documento se valida contra el maestro 46 (PARAMETROS) en la BD.
        if (request.getIdTipoArchivo() == null) {
            return new BaseResponse(3, "Tipo de documento inválido");
        }
        // Extensión permitida (whitelist).
        if (!Constante.EXT_ARCHIVO_POSTULANTE.contains(extractExtension(request.getPath()))) {
            return new BaseResponse(3, "Tipo de archivo no permitido");
        }

        // Verificar existencia y tamaño real del objeto en S3 (un solo HEAD).
        HeadObjectResponse head = clientS3.headObject(request.getPath());
        if (head == null) {
            return new BaseResponse(3, "El archivo no existe en S3");
        }
        if (head.contentLength() != null
                && head.contentLength() > Constante.MAX_TAMANIO_ARCHIVO_POSTULANTE) {
            clientS3.delete(request.getPath()); // limpiar el objeto que excede el límite
            return new BaseResponse(3, "El archivo supera el tamaño máximo permitido (10 MB)");
        }

        return requirementRepository.confirmPostulantFile(baseRequest, request);
    }

    public PostulantFileListResponse listPostulantFiles(String token, Integer idRequerimientoTalento) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_ARCHIVOS);
        return requirementRepository.listPostulantFiles(baseRequest, idRequerimientoTalento);
    }

    public RqPresignedUrlResponse generatePostulantDownloadUrl(String token, RtFileDownloadRequest request) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.LISTAR_ARCHIVOS);

        // La ruta se resuelve desde el listado del postulante (no se confía en el cliente).
        PostulantFileListResponse listResponse = requirementRepository.listPostulantFiles(baseRequest,
                request.getIdRequerimientoTalento());

        String path = null;
        if (listResponse != null && listResponse.getArchivos() != null) {
            for (PostulantFileDTO file : listResponse.getArchivos()) {
                if (file.getIdRequerimientoTalentoArchivo() != null
                        && file.getIdRequerimientoTalentoArchivo().equals(request.getIdArchivo())) {
                    path = file.getRutaArchivo();
                    break;
                }
            }
        }

        if (path == null || path.isEmpty()) {
            return new RqPresignedUrlResponse(new BaseResponse(3, "Archivo no encontrado"), null, null, null);
        }

        String fileName = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
        // Fuerza la descarga como adjunto (evita render en línea de contenido peligroso).
        String url = clientS3.generatePresignedDownloadUrl(path, fileName, 5);
        if (url == null || url.isEmpty()) {
            return new RqPresignedUrlResponse(new BaseResponse(3, "Error generando URL de descarga"), null, null, null);
        }

        return new RqPresignedUrlResponse(new BaseResponse(2, "URL generada correctamente"), url, null, fileName);
    }

    public BaseResponse removePostulantFile(String token, Integer idArchivo) {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.ELIMINAR_ARCHIVOS);
        return requirementRepository.removePostulantFile(baseRequest, idArchivo);
    }

    /**
     * Ruta (key) S3 de un archivo de postulante:
     * repositorio/{idEmpresa}/postulantes/{idRequerimientoTalento}/archivos/{archivo}.
     */
    private String buildPostulantFilePath(Integer idEmpresa, Integer idRequerimientoTalento, String fileName) {
        return Constante.RUTA_REPOSITORIO + idEmpresa
                + Constante.RUTA_RT_ARCHIVOS.replace("[ID_REQUERIMIENTO_TALENTO]", idRequerimientoTalento.toString())
                + fileName;
    }

    /** Extensión en minúsculas (sin punto) del nombre/ruta, o "" si no tiene. */
    private String extractExtension(String name) {
        if (name == null) {
            return "";
        }
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        String base = slash >= 0 ? name.substring(slash + 1) : name;
        int dot = base.lastIndexOf('.');
        return dot >= 0 ? base.substring(dot + 1).toLowerCase() : "";
    }

    /**
     * Sanea el nombre de archivo: descarta cualquier ruta, restringe a caracteres
     * seguros [A-Za-z0-9._-], limita la longitud y conserva la extensión.
     */
    private String sanitizeFileName(String originalFilename, String extension) {
        int slash = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
        String base = slash >= 0 ? originalFilename.substring(slash + 1) : originalFilename;
        int dot = base.lastIndexOf('.');
        String namePart = dot >= 0 ? base.substring(0, dot) : base;

        namePart = namePart.replaceAll("[^A-Za-z0-9._-]", "_");
        if (namePart.isEmpty()) {
            namePart = "archivo";
        }
        if (namePart.length() > 80) {
            namePart = namePart.substring(0, 80);
        }
        return extension.isEmpty() ? namePart : namePart + "." + extension;
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
