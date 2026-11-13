package org.app.autfmi.service.impl;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.dto.aprobacion.*;
import org.app.autfmi.model.report.IReport;
import org.app.autfmi.model.request.*;
import org.app.autfmi.repository.*;
import org.app.autfmi.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

/** Decisión y operación en la misma transacción, con bloqueo de fila y versión. */
@Service
@RequiredArgsConstructor
public class ApprovalAreaService {

  private final ApprovalService auth;
  private final ApprovalWorkflowService workflow;
  private final ApprovalRepository requests;
  private final ApprovalAreaRepository areas;
  private final EmployeeOperationExecutor executor;
  private final EmployeeRepository employees;
  private final HistoryRepository history;
  private final ApprovalAreaNotifier notifier;
  private final ObjectMapper mapper;

  public static class DecisionException extends RuntimeException {

    public final ApprovalOutcome outcome;

    public DecisionException(ApprovalOutcome outcome, String message) {
      super(message);
      this.outcome = outcome;
    }
  }

  private record Context(
    UserDTO user,
    ApprovalDetailDTO detail,
    ApprovalFlowDTO flow,
    JsonNode form
  ) {}

  private Context context(String token, Integer id) {
    var user = auth.authenticate(token);
    if (id == null || id < 1) throw bad("Solicitud no válida.");
    var detail = requests.getRequest(id, user, ApprovalService.rolesCsv(user));
    if (
      detail.outcome() != ApprovalOutcome.OK || detail.body().getData() == null
    ) throw new DecisionException(
      detail.outcome(),
      detail.body().getBaseResponse().getMensaje()
    );
    var flow = workflow.getFlow(token, id);
    if (
      flow.outcome() != ApprovalOutcome.OK || flow.body().getData() == null
    ) throw new DecisionException(
      flow.outcome(),
      flow.body().getBaseResponse().getMensaje()
    );
    try {
      return new Context(
        user,
        detail.body().getData(),
        flow.body().getData(),
        mapper.readTree(detail.body().getData().getDatosFormulario())
      );
    } catch (java.io.IOException e) {
      throw bad("El formulario guardado no es válido.");
    }
  }

  private List<ApprovalAreaDTO> state(Context c, boolean populateForm) {
    var saved = areas.states(c.detail.getIdSolicitud());
    var result = new ArrayList<ApprovalAreaDTO>();
    for (String name : ApprovalAreaPolicy.AREAS) {
      var row = saved.get(name);
      boolean applies = ApprovalAreaPolicy.applies(
        c.detail.getTipoSolicitud(),
        c.form,
        name
      );
      String status =
        row != null
          ? (String) row.get("ESTADO")
          : !applies
            ? "NO_APLICA"
            : "PENDIENTE".equals(c.detail.getEstado())
              ? "PENDIENTE"
              : c.detail.getEstado();
      if ("INGRESO".equals(c.detail.getTipoSolicitud()) && "EQUIPO".equals(name)
          && "PENDIENTE".equals(c.detail.getEstado()) && Set.of("PENDIENTE", "NO_APLICA").contains(status)) {
        status = applies ? "PENDIENTE" : "NO_APLICA";
      }
      if ("ANULADA".equals(c.detail.getEstado()) && "PENDIENTE".equals(status)) status = "ANULADA";
      var data = applies
        ? ApprovalAreaPolicy.data(mapper, c.detail.getTipoSolicitud(), c.form, name)
        : Map.<String, Object>of();
      if (populateForm && applies && "EQUIPO".equals(name) && "PENDIENTE".equals(status)
          && !"CESE".equals(c.detail.getTipoSolicitud())) {
        var source = ApprovalAreaPolicy.source(c.detail.getTipoSolicitud(), c.form, name);
        if (!source.hasNonNull("lstSoftware") && Set.of(1, 2).contains(source.path("idTipoEquipo").asInt())) {
          data.put("software", defaultSoftwareText());
        }
      }
      if (populateForm && applies && "FMI".equals(name) && "INGRESO".equals(c.detail.getTipoSolicitud())
          && c.detail.getIdTalento() != null) {
        // Compatibilidad con solicitudes anteriores que no guardaban estos textos.
        boolean missing = List.of("nombres", "apellidoPaterno", "apellidoMaterno", "area")
          .stream().anyMatch(key -> !data.containsKey(key) || data.get(key).toString().isBlank());
        if (missing) requests.getTalentGeneral(c.detail.getIdTalento(),
          c.form.path("idArea").isInt() ? c.form.path("idArea").asInt() : null)
          .forEach((key, value) -> {
            if (value != null && (!data.containsKey(key) || data.get(key).toString().isBlank())) data.put(key, value);
          });
      }
      if (populateForm && applies && "FMI".equals(name) && "INGRESO".equals(c.detail.getTipoSolicitud())
          && c.detail.getIdTalento() != null) {
        var selection = c.form.path("seleccion");
        if (!c.form.hasNonNull("correoPersonal") && selection.hasNonNull("email"))
          data.put("correoPersonal", selection.get("email").asText());
        if (!c.form.hasNonNull("celularPersonal") && selection.hasNonNull("celular"))
          data.put("celularPersonal", selection.get("celular").asText());
        if (!data.containsKey("correoPersonal") || !data.containsKey("celularPersonal")) {
          var contact = employees.getContactoTalento(c.detail.getIdTalento());
          if (contact != null) {
            if (contact.getCorreo() != null) data.putIfAbsent("correoPersonal", contact.getCorreo());
            if (contact.getCelular() != null) data.putIfAbsent("celularPersonal", contact.getCelular());
          }
        }
        if (!data.containsKey("tipoIngreso") && "PENDIENTE".equals(c.detail.getEstado())) {
          var entry = new EmployeeEntryRequest();
          entry.setIdTalento(c.detail.getIdTalento());
          workflow.populatePreviousEntry(c.user, entry);
          if (entry.getTipoIngreso() != null) data.put("tipoIngreso", entry.getTipoIngreso());
          if (entry.getFechaIngresoAnterior() != null) data.put("fechaIngresoAnterior", entry.getFechaIngresoAnterior());
          if (entry.getClienteAnterior() != null) data.put("clienteAnterior", entry.getClienteAnterior());
          if (entry.getCargoAnterior() != null) data.put("cargoAnterior", entry.getCargoAnterior());
        }
      }
      if (populateForm && applies && "CREDENCIALES".equals(name)) {
        var account = requests.getCorporateCredentials(c.detail.getIdTalento());
        if (account != null) {
          data.put("usuarioCorporativo", account.username());
          data.put("correoCorporativo", account.email());
        }
      }
      result.add(
        new ApprovalAreaDTO(
          name,
          status,
          row == null ? null : (String) row.get("FECHA"),
          row == null ? null : (String) row.get("USUARIO"),
          data,
          c.flow.puedeGestionar() && "PENDIENTE".equals(status)
            // La preferencia se edita en la solicitud antes de ejecutar el ingreso.
            // La disponibilidad real del aviso se valida al ejecutar la operación.
            ? ApprovalAreaPolicy.fields(c.detail.getTipoSolicitud(), name)
            : List.of()
        )
      );
    }
    return result;
  }

  @Transactional(readOnly = true)
  public ApprovalAreasDTO get(String token, Integer id) {
    var c = context(token, id);
    return new ApprovalAreasDTO(
      c.flow.version(),
      c.flow.puedeGestionar(),
      state(c, true),
      areas.reviewed(id, c.user, c.flow.version())
    );
  }

  @Transactional
  public void review(String token, Integer id, String area, Long version) {
    validArea(area);
    areas.lock(id);
    var c = context(token, id);
    check(c, version);
    areas.review(id, c.user, version, area);
  }

  private static void check(Context c, Long version) {
    if (!c.flow.puedeGestionar()) throw new DecisionException(
      ApprovalOutcome.SIN_PERMISO,
      "La solicitud no está pendiente o no tiene permiso para gestionarla."
    );
    if (
      version == null || !version.equals(c.flow.version())
    ) throw new DecisionException(
      ApprovalOutcome.CONFLICTO,
      "La solicitud cambió. Recargue y revise las áreas nuevamente."
    );
  }

  private ApprovalDecisionDTO cancel(String token, Integer id, ApprovalAreaDecisionRequest request) {
    String reason = Objects.toString(request.motivo(), "").trim();
    if (reason.isEmpty() || reason.length() > 500) throw bad("Indique el motivo de la anulación (máximo 500 caracteres).");
    areas.lock(id);
    var c = context(token, id);
    check(c, request.version());
    if (!"PENDIENTE".equals(c.detail.getEstado())) throw bad("Solo puede anular una solicitud pendiente.");
    boolean executed = state(c, false).stream().anyMatch(area -> "APROBADA".equals(area.estado()))
        || c.flow.historial().stream().anyMatch(event -> Set.of("EJECUTADA", "EJECUCION_INCIERTA", "AREA_APROBADA").contains(event.accion()));
    if (executed) throw bad("Ya se ejecutó un área o su ejecución está por confirmar. Derive la solicitud a revisión.");
    return areas.cancel(id, c.user, reason);
  }

  private String defaultSoftwareText() {
    return areas.defaultSoftware().stream().map(item -> item.getProducto()
        + (item.getProdVersion() == null || item.getProdVersion().isBlank() ? "" : " | " + item.getProdVersion()))
        .collect(java.util.stream.Collectors.joining("\n"));
  }

  private ApprovalDecisionDTO rejectAll(String token, Integer id, ApprovalAreaDecisionRequest request) {
    String reason = Objects.toString(request.motivo(), "").trim();
    if (reason.isEmpty() || reason.length() > 450) throw bad("Indique un motivo de hasta 450 caracteres.");
    areas.lock(id);
    var c = context(token, id);
    if (!c.flow.esGestor()) throw new DecisionException(ApprovalOutcome.SIN_PERMISO,
        "Solo un gestor del cliente puede desaprobar la solicitud.");
    if (!Objects.equals(request.version(), c.flow.version()) || request.version() == null)
      throw new DecisionException(ApprovalOutcome.CONFLICTO, "La solicitud cambió. Recargue y revise nuevamente.");
    if (!Set.of("PENDIENTE", "APROBADA").contains(c.detail.getEstado()))
      throw bad("La solicitud ya está cerrada.");
    var current = state(c, false);
    if (current.stream().anyMatch(a -> "FMI".equals(a.area()) && "APROBADA".equals(a.estado()))
        || c.flow.historial().stream().anyMatch(event ->
          Set.of("EJECUTADA", "EJECUCION_INCIERTA").contains(event.accion()))) {
      throw bad("La operación de FMI ya se ejecutó o está por confirmar. Derive el caso a revisión.");
    }
    boolean equipment = !"CESE".equals(c.detail.getTipoSolicitud()) && current.stream()
        .anyMatch(a -> "EQUIPO".equals(a.area()) && "APROBADA".equals(a.estado()));
    areas.initialize(id, current);
    areas.rejectAll(id, c.user, reason);
    var decision = areas.finish(id, c.user);
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override public void afterCommit() {
        notifier.notifyArea(c.detail, "SOLICITUD", "RECHAZADA", reason, null, c.user);
        if (equipment) notifier.notifyArea(c.detail, "EQUIPO", "DEVOLUCION", reason, null, c.user);
      }
    });
    return decision;
  }

  @Transactional(rollbackFor = Exception.class)
  public ApprovalDecisionDTO decide(
    String token,
    Integer id,
    ApprovalAreaDecisionRequest request
  ) {
    if (request != null && "RECHAZAR_TODO".equals(request.accion())) return rejectAll(token, id, request);
    if (request != null && "ANULAR".equals(request.accion())) return cancel(token, id, request);
    if (
      request == null ||
      request.areas() == null ||
      request.areas().isEmpty() ||
      request.areas().size() > 3
    ) throw bad("Seleccione al menos un área.");
    var selected = new LinkedHashSet<>(request.areas());
    if (selected.size() != request.areas().size()) throw bad(
      "Hay áreas repetidas."
    );
    selected.forEach(ApprovalAreaService::validArea);
    if (
      !Set.of("APROBAR", "RECHAZAR").contains(
        Objects.toString(request.accion(), "")
      )
    ) throw bad("Acción no válida.");
    String reason = request.motivo() == null ? "" : request.motivo().trim();
    if (
      reason.length() > 500 ||
      ("RECHAZAR".equals(request.accion()) && reason.isEmpty())
    ) throw bad(
      "Indique el motivo de la desaprobación (máximo 500 caracteres)."
    );
    areas.lock(id);
    var c = context(token, id);
    check(c, request.version());
    var current = state(c, false);
    var reviewed = areas.reviewed(id, c.user, request.version());
    var pending = current
      .stream()
      .filter(a -> "PENDIENTE".equals(a.estado()))
      .map(ApprovalAreaDTO::area)
      .toList();
    if (!pending.containsAll(selected)) throw new DecisionException(
      ApprovalOutcome.CONFLICTO,
      "Alguna de las áreas ya fue resuelta o no aplica."
    );
    if ("APROBAR".equals(request.accion()) && selected.contains("EQUIPO")
        && !selected.contains("CREDENCIALES")
        && current.stream().noneMatch(a -> "CREDENCIALES".equals(a.area()) && "APROBADA".equals(a.estado()))) {
      throw bad("Para aprobar Equipo, apruebe también Credenciales.");
    }
    if (!reviewed.containsAll(selected)) throw bad(
      "Revise los detalles de cada área seleccionada antes de confirmar."
    );
    var applicable = current
      .stream()
      .filter(area -> !"NO_APLICA".equals(area.estado()))
      .map(ApprovalAreaDTO::area)
      .toList();
    if (
      request.confirmarTodo() &&
      (!"APROBAR".equals(request.accion()) ||
        !reviewed.containsAll(applicable) ||
        !selected.equals(new LinkedHashSet<>(pending)))
    ) throw bad(
      "Debe revisar las áreas de esta solicitud antes de confirmar todo."
    );
    areas.initialize(id, current);
    if ("INGRESO".equals(c.detail.getTipoSolicitud())) {
      areas.syncEquipment(id, ApprovalAreaPolicy.needsEquipment(c.form));
    }
    String status = "APROBAR".equals(request.accion())
      ? "APROBADA"
      : "RECHAZADA";
    for (String area : ApprovalAreaPolicy.AREAS)
      if (selected.contains(area)) {
        Object report = "APROBADA".equals(status) ? execute(c, area) : null;
        areas.resolve(id, area, status, c.user, reason);
        var detail = c.detail;
        var user = c.user;
        TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              notifier.notifyArea(detail, area, status, reason, report, user);
            }
          }
        );
      }
    return areas.finish(id, c.user);
  }

  @Transactional
  public ApprovalDecisionDTO update(
    String token,
    Integer id,
    String area,
    ApprovalFormUpdateRequest request
  ) {
    validArea(area);
    if (request == null) throw bad("Faltan los cambios.");
    areas.lock(id);
    var c = context(token, id);
    check(c, request.getVersion());
    var current = state(c, false);
    if (
      current
        .stream()
        .noneMatch(a -> a.area().equals(area) && "PENDIENTE".equals(a.estado()))
    ) throw bad("Esta área no se puede editar.");
    Set<String> allowed = new HashSet<>();
    current
      .stream()
      .filter(a -> a.area().equals(area))
      .findFirst()
      .orElseThrow()
      .camposEditables()
      .forEach(f -> allowed.add(f.campo()));
    if (
      request.getCambios() == null ||
      !allowed.containsAll(request.getCambios().keySet())
    ) throw bad("Uno de los campos no es editable en esta área.");
    final String json;
    boolean equipmentApplies;
    try {
      String updated = ApprovalAreaPolicy.update(
        mapper,
        c.detail.getTipoSolicitud(),
        c.detail.getDatosFormulario(),
        area,
        request.getCambios()
      );
      var form = (ObjectNode) mapper.readTree(updated);
      equipmentApplies = ApprovalAreaPolicy.needsEquipment(form);
      if ("FMI".equals(area) && "INGRESO".equals(c.detail.getTipoSolicitud())
          && request.getCambios().containsKey("tieneEquipo")
          && equipmentApplies != ApprovalAreaPolicy.needsEquipment(c.form)
          && current.stream().anyMatch(a -> "EQUIPO".equals(a.area())
            && Set.of("APROBADA", "RECHAZADA").contains(a.estado()))) {
        throw bad("El área de equipo ya se resolvió. No se puede cambiar si tiene equipo propio.");
      }
      if ("EQUIPO".equals(area) && request.getCambios().containsKey("tipoEquipo")) {
        Object value = request.getCambios().get("tipoEquipo");
        if (!(value instanceof String selectedType) || selectedType.isBlank())
          throw bad("Seleccione un tipo de equipo válido.");
        int typeId = Integer.parseInt(selectedType);
        String label = areas.equipmentType(typeId);
        if (label == null) throw bad("Seleccione un tipo de equipo válido.");
        var equipment = (ObjectNode) ApprovalAreaPolicy.source(c.detail.getTipoSolicitud(), form, area);
        equipment.put("idTipoEquipo", typeId);
        equipment.put("tipoEquipo", label);
      }
      json = mapper.writeValueAsString(form);
    } catch (IllegalArgumentException | java.io.IOException e) {
      throw bad(e.getMessage());
    }
    if (c.detail.getDatosFormulario().equals(json)) throw bad(
      "No hay cambios que guardar."
    );
    areas.initialize(id, current);
    areas.update(id, json, c.user, area);
    if ("FMI".equals(area) && "INGRESO".equals(c.detail.getTipoSolicitud())) {
      areas.syncEquipment(id, equipmentApplies);
    }
    return new ApprovalDecisionDTO(id, "PENDIENTE", areas.lock(id));
  }

  private Object execute(Context c, String area) {
    String type = c.detail.getTipoSolicitud();
    try {
      var actor = Common.createBaseRequest(
        c.user,
        ApprovalWorkflowService.funcionalidades(type)
      );
      if ("FMI".equals(area)) {
        if (
          c.detail.getIdTalento() == null ||
          !c.form.path("idTalento").isInt() ||
          c.form.path("idTalento").asInt() != c.detail.getIdTalento()
        ) throw bad(
          "El formulario FMI no identifica correctamente al talento."
        );

        ObjectNode clean = ((ObjectNode) c.form).deepCopy();
        clean.remove(List.of("credenciales", "solicitudEquipo"));
        // El ingreso registra solo FMI. El equipo se registra al aprobar SU pestaña.
        if (
          clean.path("seleccion") instanceof ObjectNode selection
        ) selection.remove("solicitudEquipo");
        var result = switch (type) {
          case "INGRESO" -> executor.executeEntry(
            actor,
            mapper.treeToValue(clean, EmployeeEntryRequest.class)
          );
          case "MOVIMIENTO" -> executor.executeMovement(
            actor,
            mapper.treeToValue(clean, EmployeeMovementRequest.class),
            true
          );
          case "CESE" -> executor.executeContractEnd(
            actor,
            mapper.treeToValue(clean, EmployeeContractEndRequest.class),
            true
          );
          default -> throw bad("No existe operación FMI para este tipo.");
        };
        if (!result.ok()) throw new DecisionException(
          ApprovalOutcome.NO_PROCESABLE,
          result.response() == null
            ? "No se pudo registrar la operación."
            : result.response().getMensaje()
        );
        if (
          result.report() instanceof
            org.app.autfmi.model.report.SelectionEntryReport selectionReport
        ) {
          var records = selectionReport.getResult().getReportesIngreso();
          if (records != null && !records.isEmpty()) try {
            var record = records.get(0);
            return history.getHistoryReport(
              actor,
              c.detail.getIdTalento(),
              record.getIdTipoHistorial(),
              record.getIdHistorial(),
              false
            );
          } catch (RuntimeException e) {
            return null;
          }
        }
        return result.report();
      }
      if ("EQUIPO".equals(area) && !"CESE".equals(type)) {
        JsonNode equipment = "INGRESO".equals(type)
          ? ApprovalAreaPolicy.equipment(c.form)
          : c.form;
        if (!equipment.isObject() || equipment.path("idTipoEquipo").asInt(0) < 1
            || equipment.path("fechaEntrega").asText("").isBlank()) {
          throw bad("Complete el tipo de equipo y la fecha de entrega antes de enviar formularios.");
        }
        ObjectNode equipmentForm = ((ObjectNode) equipment).deepCopy();
        if (!equipmentForm.hasNonNull("fechaSolicitud")) {
          equipmentForm.put("fechaSolicitud", java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")).toString());
        }
        equipmentForm.remove(
          java.util.List.of("bitCelular", "bitInternetMovil")
        );
        SolicitudEquipoRequest request = mapper.treeToValue(
          equipmentForm,
          SolicitudEquipoRequest.class
        );
        request.setIdCliente(c.detail.getIdCliente());
        request.setCliente(c.detail.getCliente());
        request.setIdTalento(c.detail.getIdTalento());
        if (request.getLstSoftware() == null) request.setLstSoftware(
            Set.of(1, 2).contains(request.getIdTipoEquipo()) ? areas.defaultSoftware() : List.of());
        if ("INGRESO".equals(type)) {
          request.setIdArea(
            c.form.path("idArea").isInt() ? c.form.path("idArea").asInt() : null
          );
          request.setPuesto(c.form.path("cargo").asText(null));
          request.setCelular(equipment.path("bitCelular").asBoolean(false));
          request.setInternetMovil(
            equipment.path("bitInternetMovil").asBoolean(false)
          );
          var contact = employees.getContactoTalento(c.detail.getIdTalento());
          if (contact != null) {
            request.setNombreEmpleado(contact.getNombres());
            request.setApellidoPaternoEmpleado(contact.getApellidos());
          }
        }
        var result = employees.insertSolicitudEquipo(
          Common.createBaseRequest(c.user, Constante.REALIZAR_MOVIMIENTO),
          request
        );
        if (
          result.getBaseResponse().getIdTipoMensaje() != 2 ||
          result.getData() == null
        ) throw new DecisionException(
          ApprovalOutcome.NO_PROCESABLE,
          "No se pudo registrar la solicitud de equipo."
        );
        try {
          var report = history.getSolicitudEquipoReport(actor, c.detail.getIdTalento(), result.getData(), false);
          if (report != null && (report.getLstSoftware() == null || report.getLstSoftware().isEmpty()))
            report.setLstSoftware(request.getLstSoftware());
          return report;
        } catch (RuntimeException e) {
          return null;
        }
      }
      // Credenciales y devolución: solicitudes al área responsable, no se ejecutan cambios de acceso desde FMI.
      return null;
    } catch (DecisionException e) {
      throw e;
    } catch (Exception e) {
      throw new DecisionException(
        ApprovalOutcome.NO_PROCESABLE,
        "No se pudo registrar el área. No se confirmó ninguna decisión; recargue la solicitud."
      );
    }
  }

  private static void validArea(String area) {
    if (!ApprovalAreaPolicy.AREAS.contains(area)) throw bad("Área no válida.");
  }

  private static DecisionException bad(String text) {
    return new DecisionException(ApprovalOutcome.PARAMETRO_INVALIDO, text);
  }
}
