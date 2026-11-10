package org.app.autfmi.model.request;

import java.util.List;

public record ApprovalAreaDecisionRequest(
  Long version,
  List<String> areas,
  String accion,
  String motivo,
  boolean confirmarTodo
) {}
