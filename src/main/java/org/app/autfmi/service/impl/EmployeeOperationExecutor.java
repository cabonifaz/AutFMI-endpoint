package org.app.autfmi.service.impl;

import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.IReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.repository.HistoryRepository;
import org.app.autfmi.util.Constante;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Registra un ingreso, movimiento o cese con los SP existentes y obtiene su
 * reporte, sin enviar correos. Lo usan el registro directo (EmployeeService) y
 * la aprobación de una solicitud (FTBO-1124).
 */
@Component
@RequiredArgsConstructor
public class EmployeeOperationExecutor {

  private final HistoryRepository historyRepository;
  private final Logger logger = LoggerFactory.getLogger(EmployeeOperationExecutor.class);

  /** Respuesta del SP y, si se pudo obtener, el reporte para los correos. */
  public record Execution(BaseResponse response, IReport report) {
    public boolean ok() {
      return response != null && Integer.valueOf(2).equals(response.getIdTipoMensaje());
    }
  }

  public Execution executeEntry(BaseRequest baseRequest, EmployeeEntryRequest request) {
    EntryReport report = historyRepository.registerEntry(baseRequest, request);

    if (report == null)
      throw new IllegalArgumentException("El reporte no puede ser nulo");

    return new Execution(report.getResponse(), report.getResponse().getIdTipoMensaje() == 2 ? report : null);
  }

  public Execution executeMovement(BaseRequest baseRequest, EmployeeMovementRequest request) {
    return executeMovement(baseRequest, request, false);
  }

  /**
   * @param reporteOpcional true en la aprobación (FTBO-1124): si el movimiento ya
   *        se registró y solo falla la lectura del reporte, se informa como
   *        registrado sin reporte en vez de lanzar (lanzar haría creer que no se
   *        registró y permitiría repetirlo).
   */
  public Execution executeMovement(BaseRequest baseRequest, EmployeeMovementRequest request,
      boolean reporteOpcional) {
    OperationResult<Integer> operationResult = historyRepository.registerMovement(baseRequest, request);
    BaseResponse baseResponse = operationResult.getBaseResponse();
    Integer operationId = operationResult.getData();

    if (baseResponse.getIdTipoMensaje() != 2)
      return new Execution(baseResponse, null);

    if (operationId == null)
      return new Execution(new BaseResponse(2,
          "Movimiento confirmado, pero no se pudo generar el reporte por falta de ID de movimiento",
          "No se obtuvo ID de operación"), null);

    // Obtener reporte de movimiento usando el ID de la operación
    this.logger.info("Obteniendo reporte de movimiento para ID de operación: {}", operationId);
    Integer talentId = request.getIdTalento() == null ? 0 : request.getIdTalento();
    try {
      MovementReport report = (MovementReport) historyRepository.getHistoryReport(
          baseRequest,
          talentId,
          Constante.TIPO_REPORTE_MOVIMIENTO,
          operationId,
          false);

      return new Execution(baseResponse, report);
    } catch (RuntimeException e) {
      if (!reporteOpcional)
        throw e;
      this.logger.error("Movimiento {} registrado, pero falló su reporte: ", operationId, e);
      return new Execution(baseResponse, null);
    }
  }

  public Execution executeContractEnd(BaseRequest baseRequest, EmployeeContractEndRequest request) {
    return executeContractEnd(baseRequest, request, false);
  }

  /** @param reporteOpcional ver {@link #executeMovement(BaseRequest, EmployeeMovementRequest, boolean)}. */
  public Execution executeContractEnd(BaseRequest baseRequest, EmployeeContractEndRequest request,
      boolean reporteOpcional) {
    OperationResult<Integer> operationResult = historyRepository.registerContractTermination(baseRequest,
        request);
    BaseResponse response = operationResult.getBaseResponse();
    Integer operationId = operationResult.getData();

    if (response.getIdTipoMensaje() != 2)
      return new Execution(response, null);

    if (operationId == null) {
      String msg = "Cese confirmado, pero no se pudo generar el reporte por falta de ID de cese";
      String detail = "No se obtuvo ID de operación";
      this.logger.error("No se pudo generar  el reporte de cese: {} - {}", msg, detail);
      return new Execution(new BaseResponse(2, msg, detail), null);
    }

    // Obtener reporte de cese usando el ID de la operación
    this.logger.info("Obteniendo reporte de cese para ID de operación: {}", operationId);
    Integer talentId = request.getIdTalento() == null ? 0 : request.getIdTalento();
    try {
      CeseReport report = (CeseReport) historyRepository.getHistoryReport(baseRequest, talentId,
          Constante.TIPO_REPORTE_CESE, operationId, false);

      return new Execution(response, report);
    } catch (RuntimeException e) {
      if (!reporteOpcional)
        throw e;
      this.logger.error("Cese {} registrado, pero falló su reporte: ", operationId, e);
      return new Execution(response, null);
    }
  }
}
