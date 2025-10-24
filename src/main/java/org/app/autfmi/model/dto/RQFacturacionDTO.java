package org.app.autfmi.model.dto;

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
  private Integer declaraSunat;
  private String sedeSunat;
  private Double montoBase;
  private Double montoMovilidad;
  private Double montoMensual;
  private Double montoTrimestral;
  private Double montoSemestral;
  private Integer idEstadoRegistro;

}
