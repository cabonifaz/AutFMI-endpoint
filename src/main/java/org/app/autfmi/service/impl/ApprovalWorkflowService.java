package org.app.autfmi.service.impl;

import java.util.Arrays;
import java.util.Locale;

import org.app.autfmi.model.dto.ContactoTalentoDTO;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalDecisionDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalDetailDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalFlowDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalOutcome;
import org.app.autfmi.model.dto.aprobacion.ApprovalRegistrationDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalResult;
import org.app.autfmi.model.dto.aprobacion.ApprovedRequestDTO;
import org.app.autfmi.model.request.ApprovalApproveRequest;
import org.app.autfmi.model.request.ApprovalFormUpdateRequest;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.repository.ApprovalRepository;
import org.app.autfmi.repository.EmployeeRepository;
import org.app.autfmi.util.ApprovalFormPolicy;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Flujo de aprobación de ingresos, movimientos y ceses (FTBO-1124).
 *
 * Con approval.flow.enabled = true (APPROVAL_FLOW_ENABLED), el formulario no se
 * ejecuta al enviarlo: queda como solicitud PENDIENTE en la bandeja de los
 * gestores del cliente. Al aprobarla, un gestor asignado ejecuta la operación
 * con los SP de siempre y salen los correos de siempre a las áreas. Apagado
 * (por defecto), todo funciona como antes.
 */
@Service
public class ApprovalWorkflowService {

  private static final Logger logger = LoggerFactory.getLogger(ApprovalWorkflowService.class);

  private final ApprovalRepository approvalRepository;
  private final ApprovalService approvalService;
  private final EmployeeOperationExecutor operationExecutor;
  private final ApprovalNotifier notifier;
  private final EmployeeRepository employeeRepository;
  private final ObjectMapper objectMapper;
  private final boolean flujoActivo;

  public ApprovalWorkflowService(ApprovalRepository approvalRepository, ApprovalService approvalService,
      EmployeeOperationExecutor operationExecutor, ApprovalNotifier notifier, EmployeeRepository employeeRepository,
      ObjectMapper objectMapper, @Value("${approval.flow.enabled:false}") boolean flujoActivo) {
    this.approvalRepository = approvalRepository;
    this.approvalService = approvalService;
    this.operationExecutor = operationExecutor;
    this.notifier = notifier;
    this.employeeRepository = employeeRepository;
    this.objectMapper = objectMapper;
    this.flujoActivo = flujoActivo;
    logger.info("Flujo de aprobaciones FTBO-1124 {}.", flujoActivo ? "ACTIVO" : "desactivado");
  }

  // ------------------------------------------------------------ Registro

  /**
   * Alta que viene de Selección: la que está asociada a un requerimiento (el
   * RQ lo atiende Selección) y a un cliente con gestores. Un alta sin RQ sigue
   * el camino directo.
   */
  public boolean requiresApproval(EmployeeEntryRequest request) {
    return flujoActivo && request.getIdRequerimiento() != null && request.getIdCliente() != null;
  }

  /** Sin cliente no hay gestores a quienes enviar la solicitud: camino directo. */
  public boolean requiresApproval(EmployeeMovementRequest request) {
    return flujoActivo && request.getIdCliente() != null;
  }

  public boolean requiresApproval(EmployeeContractEndRequest request) {
    // Un cliente ausente nunca debe permitir saltar la aprobación del cese.
    return flujoActivo;
  }

  public BaseResponse submitEntry(UserDTO user, EmployeeEntryRequest request) {
    String resumen = "Ingreso desde Selección (RQ " + request.getIdRequerimiento() + ")"
        + texto(": ", request.getCargo()) + texto(", inicio ", request.getFchInicioContrato());
    return submit(user, "INGRESO", request.getIdCliente(), request.getIdTalento(),
        nombreTalento(request.getIdTalento()), resumen, request);
  }

  public BaseResponse submitMovement(UserDTO user, EmployeeMovementRequest request) {
    String nombre = unir(request.getNombres(), request.getApellidoPaterno(), request.getApellidoMaterno());
    String resumen = "Movimiento" + texto(": ", request.getPuesto()) + texto(", desde ", request.getFchMovimiento());
    return submit(user, "MOVIMIENTO", request.getIdCliente(), request.getIdTalento(), nombre, resumen, request);
  }

  public BaseResponse submitContractEnd(UserDTO user, EmployeeContractEndRequest request) {
    if (request.getIdTalento() == null || request.getContractId() == null) {
      return new BaseResponse(1, "Faltan el talento o el contrato del cese.");
    }
    Integer clienteContrato = employeeRepository.getContractClients(request.getIdTalento()).get(request.getContractId());
    if (clienteContrato == null || clienteContrato < 1) {
      return new BaseResponse(1, "No se pudo identificar el cliente del contrato. No se ejecutó el cese.");
    }
    if (request.getIdCliente() != null && !clienteContrato.equals(request.getIdCliente())) {
      return new BaseResponse(1, "El cliente enviado no corresponde al contrato. Actualice el detalle del talento.");
    }
    request.setIdCliente(clienteContrato);
    String nombre = unir(request.getNombres(), request.getApellidoPaterno(), request.getApellidoMaterno());
    String resumen = "Cese" + texto(" al ", request.getFchCese());
    return submit(user, "CESE", request.getIdCliente(), request.getIdTalento(), nombre, resumen, request);
  }

  private BaseResponse submit(UserDTO user, String tipo, Integer idCliente, Integer idTalento, String nombreTalento,
      String resumen, Object formulario) {
    String datos;
    try {
      datos = objectMapper.writeValueAsString(formulario);
    } catch (Exception e) {
      logger.error("No se pudo serializar el formulario {}: ", tipo, e);
      return new BaseResponse(3, "No se pudo registrar la solicitud de aprobación.");
    }

    // El SP exige al solicitante las mismas funcionalidades que el registro directo.
    ApprovalResult<ApprovalRegistrationDTO> result = approvalRepository.register(tipo, idCliente, idTalento,
        recortar(nombreTalento, 200), recortar(resumen, 300), datos, user, ApprovalService.rolesCsv(user),
        funcionalidades(tipo));
    BaseResponse base = result.body().getBaseResponse();
    ApprovalRegistrationDTO registro = result.body().getData();
    if (result.outcome() != ApprovalOutcome.OK || registro == null) {
      return new BaseResponse(base.getIdTipoMensaje(), base.getMensaje());
    }

    logger.info("Solicitud {} N° {} registrada por {} para {} gestores.", tipo, registro.idSolicitud(),
        user.getUsuario(), registro.gestores().size());
    notifier.notifyGestores(new ApprovalNotifier.GestorNotice(registro.idSolicitud(), tipo, registro.cliente(),
        nombreTalento, resumen, datos, registro.gestores(), user));

    String detalle = registro.gestores().isEmpty()
        ? "El cliente no tiene gestores activos asignados: revise CLIENTE_GESTOR."
        : null;
    return new BaseResponse(2, "Solicitud de aprobación N° " + registro.idSolicitud()
        + " registrada. Queda pendiente de aprobación de los gestores del cliente.", detalle);
  }

  // ------------------------------------------------------------ Bandeja

  public ApprovalResult<ApprovalFlowDTO> getFlow(String token, Integer idSolicitud) {
    UserDTO user = approvalService.authenticate(token);
    if (idSolicitud == null || idSolicitud < 1) {
      return invalido("El identificador de la solicitud no es válido.");
    }
    ApprovalResult<ApprovalFlowDTO> result = approvalRepository.getFlow(idSolicitud, user,
        ApprovalService.rolesCsv(user));
    ApprovalFlowDTO flow = result.body().getData();
    if (result.outcome() != ApprovalOutcome.OK || flow == null || !flow.puedeGestionar()) {
      return result;
    }
    return new ApprovalResult<>(result.outcome(), new OperationResult<>(
        result.body().getBaseResponse(),
        flow.withCamposEditables(ApprovalFormPolicy.editableFields(flow.tipoSolicitud()))));
  }

  public ApprovalResult<ApprovalDecisionDTO> updateForm(String token, Integer idSolicitud,
      ApprovalFormUpdateRequest request) {
    UserDTO user = approvalService.authenticate(token);
    if (idSolicitud == null || idSolicitud < 1 || request == null || request.getVersion() == null) {
      return invalido("Faltan la solicitud o su versión.");
    }
    String roles = ApprovalService.rolesCsv(user);

    // Formulario vigente, con el mismo alcance que el detalle.
    ApprovalResult<ApprovalDetailDTO> detalle = approvalRepository.getRequest(idSolicitud, user, roles);
    ApprovalDetailDTO actual = detalle.body().getData();
    if (detalle.outcome() != ApprovalOutcome.OK || actual == null) {
      return detalle.withoutData();
    }
    if (!"PENDIENTE".equals(actual.getEstado())) {
      return ApprovalResult.of(ApprovalOutcome.CONFLICTO, 1, "La solicitud ya no está pendiente: no se puede editar.", null);
    }

    ApprovalFormPolicy.AppliedChanges cambios;
    try {
      cambios = ApprovalFormPolicy.apply(objectMapper, actual.getTipoSolicitud(), actual.getDatosFormulario(),
          request.getCambios());
    } catch (IllegalArgumentException e) {
      return invalido(e.getMessage());
    }
    if (!cambios.cambio()) {
      return invalido("No hay cambios que guardar.");
    }

    // El SP vuelve a comprobar gestor asignado, estado y versión.
    return approvalRepository.updateForm(idSolicitud, request.getVersion(), cambios.datosFormulario(),
        cambios.detalle(), user, roles);
  }

  public ApprovalResult<ApprovalDecisionDTO> approve(String token, Integer idSolicitud,
      ApprovalApproveRequest request) {
    UserDTO user = approvalService.authenticate(token);
    if (idSolicitud == null || idSolicitud < 1 || request == null || request.getVersion() == null) {
      return invalido("Faltan la solicitud o su versión.");
    }

    // 1. Aprobación atómica: entre gestores simultáneos solo una pasa.
    ApprovalResult<ApprovedRequestDTO> aprobacion = approvalRepository.approve(idSolicitud, request.getVersion(),
        user, ApprovalService.rolesCsv(user));
    ApprovedRequestDTO aprobada = aprobacion.body().getData();
    if (aprobacion.outcome() != ApprovalOutcome.OK || aprobada == null) {
      return aprobacion.withoutData();
    }

    // 2. Ejecución de la operación con los SP existentes, como el gestor que aprobó.
    String tipo = aprobada.tipoSolicitud();
    String operacion = tipo.toLowerCase(Locale.ROOT);
    Object formulario;
    try {
      formulario = objectMapper.readValue(aprobada.datosFormulario(), ApprovalFormPolicy.formClass(tipo));
    } catch (Exception e) {
      // Aún no se llamó a ningún SP de la operación: devolverla a PENDIENTE es seguro.
      logger.error("Formulario ilegible en la solicitud {}: ", idSolicitud, e);
      return rechazada(idSolicitud, operacion, "El formulario guardado no es válido.", user);
    }

    EmployeeOperationExecutor.Execution ejecucion;
    try {
      ejecucion = execute(user, tipo, formulario);
    } catch (Exception e) {
      // Error de conexión, tiempo agotado o fallo tras llamar al SP: no se sabe si
      // la operación quedó registrada. Volver a PENDIENTE permitiría aprobarla otra
      // vez y duplicarla; queda APROBADA hasta que alguien la verifique.
      logger.error("Resultado incierto al ejecutar la solicitud {} aprobada: ", idSolicitud, e);
      approvalRepository.registerResult(idSolicitud, null,
          "No se pudo confirmar si el " + operacion + " se registró: " + e.getClass().getSimpleName(), user);
      return ApprovalResult.of(ApprovalOutcome.VERIFICACION_PENDIENTE, 1,
          "La solicitud quedó aprobada, pero no se pudo confirmar si el " + operacion
              + " se registró. No lo vuelva a registrar: revise el historial del talento.", null);
    }

    if (!ejecucion.ok()) {
      // 3a. El SP lo rechazó (no se registró): vuelve a PENDIENTE para corregir o reintentar.
      String motivo = ejecucion.response() != null ? ejecucion.response().getMensaje() : null;
      return rechazada(idSolicitud, operacion,
          motivo == null || motivo.isBlank() ? "Error al registrar la operación." : motivo, user);
    }

    // 3b. Correcto: queda en el historial y salen los correos a las áreas.
    approvalRepository.registerResult(idSolicitud, true,
        "Operación registrada: " + ejecucion.response().getMensaje(), user);
    notifier.notifyAreas(idSolicitud, tipo, ejecucion.report(), aprobada.idTalento(), user);

    return ApprovalResult.of(ApprovalOutcome.OK, 2,
        "Solicitud N° " + idSolicitud + " aprobada y " + operacion + " registrado.",
        new ApprovalDecisionDTO(idSolicitud, "APROBADA", null));
  }

  private <T> ApprovalResult<T> rechazada(Integer idSolicitud, String operacion, String mensaje, UserDTO user) {
    approvalRepository.registerResult(idSolicitud, false, mensaje, user);
    return ApprovalResult.of(ApprovalOutcome.NO_PROCESABLE, 1,
        "No se pudo registrar el " + operacion + ": " + mensaje + " La solicitud volvió a pendiente.", null);
  }

  /** Mismas funcionalidades que usa el registro directo de cada operación. */
  static String funcionalidades(String tipo) {
    return switch (tipo) {
      case "INGRESO" -> String.join(",", Constante.INSERTAR_TALENTO, Constante.REALIZAR_INGRESO);
      case "MOVIMIENTO" -> Constante.REALIZAR_MOVIMIENTO;
      case "CESE" -> Constante.REALIZAR_CESE;
      default -> throw new IllegalArgumentException("Tipo de solicitud no válido: " + tipo);
    };
  }

  private EmployeeOperationExecutor.Execution execute(UserDTO user, String tipo, Object formulario) {
    var baseRequest = Common.createBaseRequest(user, funcionalidades(tipo));
    return switch (tipo) {
      case "INGRESO" -> operationExecutor.executeEntry(baseRequest, (EmployeeEntryRequest) formulario);
      case "MOVIMIENTO" -> operationExecutor.executeMovement(baseRequest, (EmployeeMovementRequest) formulario, true);
      case "CESE" -> operationExecutor.executeContractEnd(baseRequest, (EmployeeContractEndRequest) formulario, true);
      default -> throw new IllegalArgumentException("Tipo de solicitud no válido: " + tipo);
    };
  }

  // ------------------------------------------------------------ Apoyo

  private String nombreTalento(Integer idTalento) {
    try {
      ContactoTalentoDTO contacto = employeeRepository.getContactoTalento(idTalento);
      return contacto == null ? null : unir(contacto.getNombres(), contacto.getApellidos());
    } catch (Exception e) {
      logger.warn("No se pudo obtener el nombre del talento {}: {}", idTalento, e.getMessage());
      return null;
    }
  }

  private static String unir(String... partes) {
    // Arrays.stream y no List.of: el apellido materno puede venir nulo.
    String unido = String.join(" ", Arrays.stream(partes)
        .filter(p -> p != null && !p.isBlank()).map(String::trim).toList());
    return unido.isEmpty() ? null : unido;
  }

  private static String texto(String prefijo, String valor) {
    return valor == null || valor.isBlank() ? "" : prefijo + valor.trim();
  }

  private static String recortar(String valor, int max) {
    return valor == null || valor.length() <= max ? valor : valor.substring(0, max);
  }

  private static <T> ApprovalResult<T> invalido(String mensaje) {
    return ApprovalResult.of(ApprovalOutcome.PARAMETRO_INVALIDO, 1, mensaje, null);
  }
}
