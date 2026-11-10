package org.app.autfmi.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.aprobacion.*;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.*;
import org.app.autfmi.service.impl.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/approval")
@RequiredArgsConstructor
public class ApprovalAreaController {

  private final ApprovalAreaService service;

  @GetMapping("/{id}/areas")
  public ResponseEntity<?> get(
    @PathVariable Integer id,
    HttpServletRequest http
  ) {
    return call(() -> service.get(ApprovalController.bearerToken(http), id));
  }

  @PostMapping("/{id}/areas/{area}/review")
  public ResponseEntity<?> review(
    @PathVariable Integer id,
    @PathVariable String area,
    @RequestBody ApprovalApproveRequest request,
    HttpServletRequest http
  ) {
    return call(() -> {
      service.review(
        ApprovalController.bearerToken(http),
        id,
        area,
        request.getVersion()
      );
      return null;
    });
  }

  @PostMapping("/{id}/areas/decision")
  public ResponseEntity<?> decide(
    @PathVariable Integer id,
    @RequestBody ApprovalAreaDecisionRequest request,
    HttpServletRequest http
  ) {
    return call(() ->
      service.decide(ApprovalController.bearerToken(http), id, request)
    );
  }

  @PutMapping("/{id}/areas/{area}/form")
  public ResponseEntity<?> update(
    @PathVariable Integer id,
    @PathVariable String area,
    @RequestBody ApprovalFormUpdateRequest request,
    HttpServletRequest http
  ) {
    return call(() ->
      service.update(ApprovalController.bearerToken(http), id, area, request)
    );
  }

  private static <T> ResponseEntity<?> call(Supplier<T> action) {
    try {
      return ResponseEntity.ok(
        new OperationResult<>(
          new BaseResponse(2, "Operación realizada correctamente."),
          action.get()
        )
      );
    } catch (ApprovalAreaService.DecisionException e) {
      return ApprovalController.toResponse(
        ApprovalResult.of(e.outcome, 1, e.getMessage(), null)
      );
    } catch (ApprovalService.InvalidSessionException e) {
      return ApprovalController.unauthorized(e);
    }
  }
}
