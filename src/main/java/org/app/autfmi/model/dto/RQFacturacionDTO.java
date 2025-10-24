package org.app.autfmi.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RQFacturacionDTO {
  private Integer idRequerimientoFacturacion;
  private Integer idRequerimiento;
  private Integer idModalidad;
  private Integer idGrupoModalidad;
  private Boolean declaraSunat;
  private String sedeSunat;
  private BigDecimal montoBase;
  private BigDecimal montoMovilidad;
  private BigDecimal montoMensual;
  private BigDecimal montoTrimestral;
  private BigDecimal montoSemestral;
  private Integer idEstadoRegistro;

  // Opcionales o informaciones extra
  private String nombreModalidad;
  private String nombreGrupoModalidad;

}
