package org.app.autfmi.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementDetailsReport {
  private String titulo;
  private String descripcion;
  private String codigoRQ;
  private Date fechaSolicitud;
  private Date fechaVencimiento;
  private String estadoRQ;
  private String modalidadRQ;
  private String duracionRQ;
  private String cliente;
}