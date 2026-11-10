package org.app.autfmi.model.dto.aprobacion;

import java.util.List;

public record ApprovalAreasDTO(
  Long version,
  boolean puedeGestionar,
  List<ApprovalAreaDTO> areas,
  List<String> revisadas
) {}
