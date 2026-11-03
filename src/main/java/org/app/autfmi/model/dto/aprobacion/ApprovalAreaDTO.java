package org.app.autfmi.model.dto.aprobacion;

import java.util.List;
import java.util.Map;

public record ApprovalAreaDTO(
  String area,
  String estado,
  String fechaResolucion,
  String usuario,
  Map<String, Object> datos,
  List<ApprovalEditableFieldDTO> camposEditables
) {}
