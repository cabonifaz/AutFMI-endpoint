package org.app.autfmi.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.app.autfmi.model.dto.FileDTO;
import org.app.autfmi.model.dto.FormularioConfirmacionDTO;
import org.app.autfmi.model.dto.GestorClienteDTO;
import org.app.autfmi.model.dto.GestorRqDTO;
import org.app.autfmi.model.dto.PostulantDTO;
import org.app.autfmi.model.dto.RequirementTalentsResult;
import org.app.autfmi.model.dto.TipoFormulario;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.IReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.report.SolicitudEquipoReport;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.EmailConfirmationRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.FormularioConfirmacionRepository;
import org.app.autfmi.repository.HistoryRepository;
import org.app.autfmi.repository.RequirementRepository;
import org.app.autfmi.service.IMailService;
import org.app.autfmi.service.IRequirementConfirmationService;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.EmailUtils;
import org.app.autfmi.util.FormularioCorreoUtils;
import org.app.autfmi.util.MailUtils;
import org.app.autfmi.util.SafeValues;
import org.app.autfmi.util.builders.ReportPDFBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Confirmación de formularios por correo.
 *
 * <ol>
 * <li>Al generar formularios (Finalizar en Asignar Talento, o ingreso,
 * movimiento, cese y solicitud de equipo en FMI) quedan pendientes y se envía
 * el correo de confirmación al gestor y suplente del cliente, con
 * {@code [TIPO | ID | CODIGO]} en el asunto y los formularios en PDF adjuntos
 * (los mismos que saldrán al confirmar). Si quien los genera ya es gestor o
 * suplente del cliente, se envían directo.</li>
 * <li>El gestor/suplente responde; n8n (IMAP + IA) detecta la confirmación y
 * llama a {@link #processEmailConfirmation}.</li>
 * <li>El SP valida el formulario y el remitente y lo reclama (0 -> 2); se envían
 * los formularios y se marca 1 (enviados) o 0 (reintentar).</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class RequirementConfirmationService implements IRequirementConfirmationService {

    private final Logger logger = LoggerFactory.getLogger(RequirementConfirmationService.class);

    // Formularios que envía NotificationService al confirmar (mismos nombres que sus correos).
    private static final String FORM_INGRESO = "Formulario de ingreso";
    private static final String FORM_USUARIO = "Solicitud de creación de usuario";
    private static final String FORM_EQUIPO = "Solicitud de equipo";

    private final RequirementRepository requirementRepository;
    private final FormularioConfirmacionRepository formularioRepository;
    private final HistoryRepository historyRepository;
    private final NotificationService notificationService;
    private final IMailService mailService;
    private final MailUtils mailUtils;
    private final ReportPDFBuilder reportPDFBuilder;

    // ------------------------------------------------------------------ solicitud

    @Override
    public ResultadoSolicitud requestConfirmation(Integer idRequerimiento, RequirementTalentsResult result,
            Integer idUsuarioActor, BaseRequest baseRequest) {
        List<GestorClienteDTO> gestoresCliente = result.getGestoresCliente() != null
                ? result.getGestoresCliente()
                : Collections.emptyList();

        // Quien finalizó es gestor o suplente del cliente: no hay nada que confirmar
        // (el SP ya dejó a los talentos con ESTADO_FORMULARIOS = 1).
        if (idUsuarioActor != null && gestoresCliente.stream().anyMatch(g -> idUsuarioActor.equals(g.getIdUsuario()))) {
            logger.info("RQ {}: finalizó el gestor/suplente del cliente; formularios directos.", idRequerimiento);
            return ResultadoSolicitud.DIRECTO;
        }

        GestorRqDTO actor = result.getGestorRq();
        String cliente = actor != null ? SafeValues.safeString(actor.getCliente()) : "";
        String codigoRq = actor != null ? SafeValues.safeString(actor.getCodigoRQ()) : "";
        String solicitante = actor != null
                ? (SafeValues.safeString(actor.getNombres()) + " " + SafeValues.safeString(actor.getApellidos())).trim()
                : "";

        Map<String, Object> variables = new HashMap<>();
        variables.put("solicitante", solicitante);
        variables.put("codigoRq", codigoRq);
        variables.put("cliente", cliente);
        // Requisitos de ingreso del RQ (EMO, SCTR, asignación de laptop)
        variables.put("requisitos", actor != null
                ? FormularioCorreoUtils.requisitosIngreso(actor.getEmo(), actor.getSctr(), actor.getAsignacionLaptop())
                : null);
        // Qué formularios saldrán al confirmar: ingreso y creación de usuario para
        // cada talento ingresado; solicitud de equipo solo para quien la tiene.
        Set<Integer> conEquipo = new HashSet<>();
        if (result.getReportesSolicitudEquipo() != null) {
            result.getReportesSolicitudEquipo().forEach(r -> conEquipo.add(r.getIdTalento()));
        }
        List<Map<String, Object>> filas = buildFilas(result.getPostulantes(), conEquipo);
        long totalEquipo = filas.stream().filter(f -> Boolean.TRUE.equals(f.get("equipo"))).count();

        variables.put("filas", filas);
        variables.put("totalTalentos", filas.size());
        variables.put("totalEquipo", totalEquipo);

        TipoFormulario tipo = TipoFormulario.RQ;
        String subject = String.format(Constante.ASUNTO_CONFIRMACION,
                tipo.getEtiqueta(), cliente, tipo.name(), idRequerimiento, codigoRq);

        return sendConfirmationMail(gestoresCliente, actor != null ? actor.getCorreo() : null, subject,
                Constante.PLANTILLA_CONFIRMACION_INGRESO, variables, "RQ " + idRequerimiento,
                () -> adjuntosRequerimiento(result, baseRequest));
    }

    /**
     * Formularios de la tanda de Asignar Talento, generados igual que en
     * {@link NotificationService#sendRequirementNotificationsSync}: ingreso y
     * creación de usuario por talento + solicitudes de equipo.
     */
    private List<FileDTO> adjuntosRequerimiento(RequirementTalentsResult result, BaseRequest baseRequest) {
        List<FileDTO> files = new ArrayList<>();
        if (baseRequest == null) {
            return files;
        }
        if (result.getReportesIngreso() != null) {
            for (RequirementTalentsResult.ReporteIngreso r : result.getReportesIngreso()) {
                if (r.getIdTalento() == null) {
                    continue;
                }
                IReport report = historyRepository.getHistoryReport(
                        baseRequest, r.getIdTalento(), r.getIdTipoHistorial(), r.getIdHistorial(), false);
                if (report instanceof EntryReport entry) {
                    files.addAll(reportPDFBuilder.adjuntosIngreso(entry));
                }
            }
        }
        if (result.getReportesSolicitudEquipo() != null) {
            for (RequirementTalentsResult.ReporteSolicitudEquipo s : result.getReportesSolicitudEquipo()) {
                SolicitudEquipoReport report = historyRepository.getSolicitudEquipoReport(
                        baseRequest, s.getIdTalento(), s.getIdSolicitudEquipo(), false);
                files.addAll(reportPDFBuilder.adjuntosEquipo(report));
            }
        }
        return files;
    }

    @Override
    public ResultadoSolicitud requestFormConfirmation(TipoFormulario tipo, Integer id, Integer idTalento,
            Integer idCliente, Integer idUsuarioActor, String talento, Supplier<List<FileDTO>> adjuntos) {
        // Sin cliente (p. ej. ingreso de personal interno) no hay gestor que confirme.
        if (idCliente == null || idCliente <= 0) {
            logger.info("{} del talento {}: sin cliente, formularios directos.", tipo, idTalento);
            return ResultadoSolicitud.DIRECTO;
        }

        FormularioConfirmacionDTO.Registro registro;
        try {
            registro = formularioRepository.registrar(tipo, id, idTalento, idCliente, idUsuarioActor);
        } catch (Exception e) {
            logger.error("{} del talento {}: no se pudo registrar la confirmación: ", tipo, idTalento, e);
            return ResultadoSolicitud.ERROR_ENVIO;
        }

        if (registro.getBaseResponse().getIdTipoMensaje() != 2) {
            logger.error("{} del talento {}: no se pudo registrar la confirmación: {}", tipo, idTalento,
                    registro.getBaseResponse().getMensaje());
            return ResultadoSolicitud.ERROR_ENVIO;
        }

        if (registro.isEsGestor()) {
            logger.info("{} {}: lo generó el gestor/suplente del cliente; formularios directos.",
                    tipo, registro.getIdFormulario());
            return ResultadoSolicitud.DIRECTO;
        }

        String cliente = SafeValues.safeString(registro.getCliente());
        Integer idFormulario = registro.getIdFormulario();

        Map<String, Object> variables = new HashMap<>();
        variables.put("etiqueta", tipo.getEtiqueta());
        variables.put("solicitante", SafeValues.safeString(registro.getSolicitante()));
        variables.put("cliente", cliente);
        variables.put("talento", SafeValues.safeString(talento));
        variables.put("formularios", tipo.getFormularios());

        String subject = String.format(Constante.ASUNTO_CONFIRMACION,
                tipo.getEtiqueta(), cliente, tipo.name(), idFormulario, tipo.codigo(idFormulario));

        return sendConfirmationMail(registro.getGestores(), registro.getCorreoSolicitante(), subject,
                Constante.PLANTILLA_CONFIRMACION_FORMULARIO, variables, tipo + " " + idFormulario, adjuntos);
    }

    @Override
    public String describeResultado(ResultadoSolicitud resultado) {
        if (resultado == null) {
            return null;
        }
        switch (resultado) {
            case ENVIADA:
                return "Registro guardado. Los formularios se enviarán cuando el gestor o el suplente"
                        + " del cliente confirmen por correo.";
            case SIN_GESTORES:
                return "Registro guardado, pero el cliente no tiene gestor ni suplente con correo para"
                        + " confirmar. Los formularios quedaron pendientes de confirmación.";
            case ERROR_ENVIO:
                return "Registro guardado, pero no se pudo solicitar la confirmación al gestor."
                        + " Los formularios quedaron pendientes de confirmación.";
            default:
                return null;
        }
    }

    /**
     * Envía el correo de confirmación: TO = gestor (o el primero con correo
     * válido); CC = suplente + quien generó el formulario. Adjunta los PDFs de
     * {@code adjuntos}; si no se pueden generar, el correo sale igual sin ellos.
     */
    private ResultadoSolicitud sendConfirmationMail(List<GestorClienteDTO> gestoresCliente, String correoSolicitante,
            String subject, String plantilla, Map<String, Object> variables, String referencia,
            Supplier<List<FileDTO>> adjuntos) {
        // Gestor (PRIORIDAD 1) primero, luego suplente; solo correos válidos.
        List<GestorClienteDTO> gestores = new ArrayList<>(
                gestoresCliente != null ? gestoresCliente : Collections.emptyList());
        gestores.removeIf(g -> !EmailUtils.isValidEmail(g.getCorreo() != null ? g.getCorreo().trim() : null));
        gestores.sort(Comparator.comparing(GestorClienteDTO::getPrioridad,
                Comparator.nullsLast(Comparator.naturalOrder())));

        if (gestores.isEmpty()) {
            logger.warn("{}: el cliente no tiene gestor ni suplente con correo válido; formularios pendientes.",
                    referencia);
            return ResultadoSolicitud.SIN_GESTORES;
        }

        String to = gestores.get(0).getCorreo().trim();
        List<String> cc = new ArrayList<>();
        for (int i = 1; i < gestores.size(); i++) {
            cc.add(gestores.get(i).getCorreo().trim());
        }
        if (correoSolicitante != null && !correoSolicitante.isBlank()) {
            cc.add(correoSolicitante.trim());
        }

        // Los PDFs se generan recién aquí: solo cuando de verdad se pide confirmación.
        List<FileDTO> files = Collections.emptyList();
        if (adjuntos != null) {
            try {
                List<FileDTO> generados = adjuntos.get();
                if (generados != null) {
                    files = generados;
                }
            } catch (Exception e) {
                logger.error("{}: no se pudieron generar los formularios para adjuntar; se envía sin adjuntos: ",
                        referencia, e);
            }
        }
        variables.put("adjuntos", files.size());

        logger.info("{}: enviando solicitud de confirmación -> TO: {} | CC: {} | adjuntos: {}",
                referencia, to, cc, files.size());
        boolean enviado = mailUtils.sendTemplateEmailSync(to, cc, subject, plantilla, variables, files);
        return enviado ? ResultadoSolicitud.ENVIADA : ResultadoSolicitud.ERROR_ENVIO;
    }

    // ------------------------------------------------------------------ confirmación (n8n)

    @Override
    public BaseResponse processEmailConfirmation(EmailConfirmationRequest request) {
        if (request == null || request.getId() == null
                || request.getCodigo() == null || request.getCodigo().isBlank()
                || request.getEmailRemitente() == null || request.getEmailRemitente().isBlank()) {
            return new BaseResponse(3, "Datos incompletos: se requiere tipo, id, codigo y emailRemitente.");
        }

        TipoFormulario tipo = TipoFormulario.from(request.getTipo());
        if (tipo == null) {
            return new BaseResponse(3, "Tipo de confirmación no válido: '" + request.getTipo() + "'.");
        }

        logger.info("{} {}: confirmación recibida de {} (código {}).", tipo, request.getId(),
                request.getEmailRemitente(), request.getCodigo());

        return tipo == TipoFormulario.RQ
                ? processRequirementConfirmation(request.getId(), request.getCodigo().trim(),
                        request.getEmailRemitente().trim())
                : processFormConfirmation(tipo, request.getId(), request.getCodigo().trim(),
                        request.getEmailRemitente().trim());
    }

    /** Asignar Talento: valida RQ + código + remitente y envía los formularios de la tanda. */
    private BaseResponse processRequirementConfirmation(Integer idRequerimiento, String codigoRq, String email) {
        RequirementTalentsResult result = requirementRepository.processConfirmation(idRequerimiento, codigoRq, email);

        BaseResponse baseResponse = result.getBaseResponse();
        // 1 = sin pendientes (ya confirmado), 3 = rechazado (RQ/código/remitente).
        if (baseResponse.getIdTipoMensaje() != 2) {
            logger.info("RQ {}: confirmación no procesada ({}): {}", idRequerimiento,
                    baseResponse.getIdTipoMensaje(), baseResponse.getMensaje());
            return baseResponse;
        }

        // A partir de aquí los talentos están reclamados (ESTADO_FORMULARIOS = 2):
        // pase lo que pase hay que cerrar el reclamo con MARCAR.
        List<Integer> reclamados = result.getTalentosReclamados();
        boolean enviado = false;
        try {
            if (result.getBaseRequestFinaliza() == null) {
                logger.error("RQ {}: sin datos del usuario que finalizó; no se pueden generar los formularios.",
                        idRequerimiento);
            } else {
                enviado = notificationService.sendRequirementNotificationsSync(
                        result.getGestorRq(),
                        result.getCcList() != null ? result.getCcList() : Collections.emptyList(),
                        result.getPostulantes(),
                        result.getReportesIngreso(),
                        result.getReportesSolicitudEquipo(),
                        result.getBaseRequestFinaliza());
            }
        } catch (Exception e) {
            logger.error("RQ {}: error enviando los formularios tras la confirmación: ", idRequerimiento, e);
        } finally {
            BaseResponse marca = requirementRepository.markConfirmation(idRequerimiento, reclamados, enviado);
            if (marca.getIdTipoMensaje() != 2) {
                // Los talentos quedan en 2 (en proceso): hay que revisarlos a mano.
                logger.error("RQ {}: no se pudo marcar el estado de los formularios (talentos {}): {}",
                        idRequerimiento, reclamados, marca.getMensaje());
            }
        }

        return buildResultado(enviado, describeConfirmador(result.getConfirmador()),
                "Formularios enviados para " + (reclamados != null ? reclamados.size() : 0) + " talento(s).");
    }

    /** Ingreso, movimiento, cese o solicitud de equipo generados desde FMI. */
    private BaseResponse processFormConfirmation(TipoFormulario tipo, Integer id, String codigo, String email) {
        if (!tipo.codigo(id).equalsIgnoreCase(codigo)) {
            return new BaseResponse(3, "El código '" + codigo + "' no corresponde al " + tipo + " " + id + ".");
        }

        FormularioConfirmacionDTO.Proceso proceso = formularioRepository.procesar(tipo, id, email);
        BaseResponse baseResponse = proceso.getBaseResponse();
        if (baseResponse.getIdTipoMensaje() != 2) {
            logger.info("{} {}: confirmación no procesada ({}): {}", tipo, id,
                    baseResponse.getIdTipoMensaje(), baseResponse.getMensaje());
            return baseResponse;
        }

        // Reclamado (ESTADO_FORMULARIOS = 2): siempre cerrar con MARCAR.
        boolean enviado = false;
        try {
            enviado = sendForm(tipo, id, proceso.getIdTalento(), proceso.getBaseRequestGenera());
        } catch (Exception e) {
            logger.error("{} {}: error enviando los formularios tras la confirmación: ", tipo, id, e);
        } finally {
            BaseResponse marca = formularioRepository.marcar(tipo, id, enviado);
            if (marca.getIdTipoMensaje() != 2) {
                logger.error("{} {}: no se pudo marcar el estado del formulario: {}", tipo, id, marca.getMensaje());
            }
        }

        return buildResultado(enviado, describeConfirmador(proceso.getConfirmador()), "Formularios enviados.");
    }

    /** Regenera el reporte con los permisos de quien generó el formulario y lo envía. */
    private boolean sendForm(TipoFormulario tipo, Integer id, Integer idTalento, BaseRequest baseRequest) {
        if (baseRequest == null || idTalento == null) {
            logger.error("{} {}: sin datos del usuario que lo generó; no se puede regenerar el formulario.", tipo, id);
            return false;
        }

        if (tipo == TipoFormulario.EQUIPO) {
            SolicitudEquipoReport report = historyRepository.getSolicitudEquipoReport(baseRequest, idTalento, id, false);
            return mailService.sendEquipmentRequestNotificationSync(report, idTalento);
        }

        IReport report = historyRepository.getHistoryReport(baseRequest, idTalento, tipo.getIdTipoHistorial(), id, false);
        switch (tipo) {
            case INGRESO:
                if (report instanceof EntryReport entry) {
                    return mailService.sendEntryReportNotificationSync(entry, idTalento);
                }
                break;
            case MOVIMIENTO:
                if (report instanceof MovementReport movement) {
                    return mailService.sendMovementReportNotificationSync(movement);
                }
                break;
            case CESE:
                if (report instanceof CeseReport cese) {
                    return mailService.sendCeseReportNotificationSync(cese);
                }
                break;
            default:
                break;
        }
        logger.error("{} {}: no se pudo regenerar el reporte del formulario.", tipo, id);
        return false;
    }

    private BaseResponse buildResultado(boolean enviado, String confirmador, String detalleExito) {
        if (!enviado) {
            BaseResponse error = new BaseResponse(3,
                    "Confirmación de " + confirmador + " recibida, pero no se pudieron enviar los formularios."
                            + " Quedaron pendientes para un nuevo intento.");
            error.setDetalleMensaje(Constante.CONFIRMACION_REINTENTAR);
            return error;
        }
        return new BaseResponse(2, "Confirmación de " + confirmador + " registrada. " + detalleExito);
    }

    /**
     * Filas de la tabla del correo de confirmación de Asignar Talento, con textos
     * ya saneados y los formularios que se enviarán para cada talento.
     */
    private List<Map<String, Object>> buildFilas(List<PostulantDTO> postulantes, Set<Integer> conEquipo) {
        List<Map<String, Object>> filas = new ArrayList<>();
        if (postulantes == null) {
            return filas;
        }
        for (PostulantDTO p : postulantes) {
            boolean equipo = p.getIdTalento() != null && conEquipo.contains(p.getIdTalento());
            List<String> formularios = new ArrayList<>();
            formularios.add(FORM_INGRESO);
            formularios.add(FORM_USUARIO);
            if (equipo) {
                formularios.add(FORM_EQUIPO);
            }

            Map<String, Object> fila = new HashMap<>();
            fila.put("nombre", (SafeValues.safeString(p.getNombres()) + " "
                    + SafeValues.safeString(p.getApellidos())).trim());
            fila.put("dni", SafeValues.safeString(p.getDni()));
            fila.put("cargo", SafeValues.safeString(p.getCargo()));
            fila.put("inicio", SafeValues.safeString(p.getFechaInicioLabores()));
            fila.put("modalidad", SafeValues.safeString(p.getModalidad()));
            fila.put("equipo", equipo);
            fila.put("formularios", formularios);
            filas.add(fila);
        }
        return filas;
    }

    private String describeConfirmador(GestorClienteDTO confirmador) {
        if (confirmador == null) {
            return "el gestor";
        }
        String rol = Integer.valueOf(2).equals(confirmador.getPrioridad()) ? "suplente" : "gestor";
        String nombre = (SafeValues.safeString(confirmador.getNombres()) + " "
                + SafeValues.safeString(confirmador.getApellidos())).trim();
        return nombre.isEmpty() ? "el " + rol : nombre + " (" + rol + ")";
    }
}
