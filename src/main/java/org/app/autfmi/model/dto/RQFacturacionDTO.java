package org.app.autfmi.model.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RQFacturacionDTO {
  private Integer idRequerimientoFacturacion;
  private Integer idRequerimiento;
  private Integer idModalidad;
  private Integer idGrupoModalidad;

  private Integer currencyType;

  // Basic Amount
  private BigDecimal minBaseAmount;
  private BigDecimal maxBaseAmount;

  // Travel Allowance / Mobility
  private BigDecimal minTravelAllowance;
  private BigDecimal maxTravelAllowance;

  // Monthly Frequency
  private BigDecimal minMonthlyAmount;
  private BigDecimal maxMonthlyAmount;

  // Quarterly Frequency (Every 3 months)
  private BigDecimal minQuarterlyAmount;
  private BigDecimal maxQuarterlyAmount;

  // Semi-Annual Frequency (Every 6 months)
  private BigDecimal minSemiAnnualAmount;
  private BigDecimal maxSemiAnnualAmount;

  private Integer idEstadoRegistro;

  // Opcionales o informaciones extra
  private String nombreModalidad;
  private String nombreGrupoModalidad;

}
