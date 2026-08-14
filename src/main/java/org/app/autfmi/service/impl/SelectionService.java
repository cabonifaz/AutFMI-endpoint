package org.app.autfmi.service.impl;

import java.util.List;

import org.app.autfmi.model.dto.seleccion.SelectionInterviewsDTO;
import org.app.autfmi.model.dto.seleccion.SelectionLabelCountDTO;
import org.app.autfmi.model.dto.seleccion.SelectionPerformanceRowDTO;
import org.app.autfmi.model.dto.seleccion.SelectionSummaryDTO;
import org.app.autfmi.model.dto.seleccion.SelectionUserDTO;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.SelectionFilterRequest;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.repository.SelectionRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/** Estadísticas del módulo Selección. La agregación vive en los SP. */
@Service
@RequiredArgsConstructor
public class SelectionService {

  private final SelectionRepository selectionRepository;

  public OperationResult<SelectionSummaryDTO> getResumen(SelectionFilterRequest request, BaseRequest baseRequest) {
    return this.selectionRepository.getResumen(request, baseRequest);
  }

  public OperationResult<SelectionInterviewsDTO> getEntrevistas(SelectionFilterRequest request, BaseRequest baseRequest) {
    return this.selectionRepository.getEntrevistas(request, baseRequest);
  }

  public OperationResult<List<SelectionLabelCountDTO>> getIngresos(SelectionFilterRequest request, BaseRequest baseRequest) {
    return this.selectionRepository.getIngresos(request, baseRequest);
  }

  public OperationResult<List<SelectionPerformanceRowDTO>> getRendimiento(SelectionFilterRequest request, BaseRequest baseRequest) {
    return this.selectionRepository.getRendimiento(request, baseRequest);
  }

  public OperationResult<List<SelectionUserDTO>> getUsuarios(String filtro, BaseRequest baseRequest) {
    return this.selectionRepository.getUsuarios(filtro, baseRequest);
  }
}
