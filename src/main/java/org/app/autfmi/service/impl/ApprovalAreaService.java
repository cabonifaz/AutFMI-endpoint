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

  private List<ApprovalAreaDTO> state(Context c) {
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
      result.add(
        new ApprovalAreaDTO(
          name,
          status,
          row == null ? null : (String) row.get("FECHA"),
          row == null ? null : (String) row.get("USUARIO"),
          applies
            ? ApprovalAreaPolicy.data(
                mapper,
                c.detail.getTipoSolicitud(),
                c.form,
                name
              )
            : Map.of(),
          c.flow.puedeGestionar() && "PENDIENTE".equals(status)
            ? "FMI".equals(name)
              ? c.flow
                  .camposEditables()
                  .stream()
                  .filter(f -> !"fchDevolucionEquipo".equals(f.campo()))
                  .toList()
              : ApprovalAreaPolicy.fields(c.detail.getTipoSolicitud(), name)
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
      state(c),
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

  @Transactional(rollbackFor = Exception.class)
  public ApprovalDecisionDTO decide(
    String token,
    Integer id,
    ApprovalAreaDecisionRequest request
  ) {
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
    var current = state(c);
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
    var current = state(c);
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
    try {
      json = ApprovalAreaPolicy.update(
        mapper,
        c.detail.getTipoSolicitud(),
        c.detail.getDatosFormulario(),
        area,
        request.getCambios()
      );
    } catch (IllegalArgumentException e) {
      throw bad(e.getMessage());
    }
    if (c.detail.getDatosFormulario().equals(json)) throw bad(
      "No hay cambios que guardar."
    );
    areas.initialize(id, current);
    areas.update(id, json, c.user, area);
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
        clean.remove("credenciales");
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
        ObjectNode equipmentForm = ((ObjectNode) equipment).deepCopy();
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
        if (request.getLstSoftware() == null) request.setLstSoftware(List.of());
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
          return history.getSolicitudEquipoReport(
            actor,
            c.detail.getIdTalento(),
            result.getData(),
            false
          );
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
