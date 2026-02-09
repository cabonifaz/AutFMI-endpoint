package org.app.autfmi.model.dto;

import java.util.List;

import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.SolicitudEquipoReport;
import org.app.autfmi.model.response.BaseResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequirementTalentsResult {
  private BaseResponse baseResponse;
  private List<PostulantDTO> postulantes;
  private List<String> contacts;
  private GestorRqDTO gestorRq;
  private List<EntryReport> entryReports;
  private List<String> copyTo;
  private List<SolicitudEquipoReport> solicitudesEquipo;
  private GestorDTO gestorCliente;

  private String gestorDocCorreo;
  private String gestorDocNombre;

}
