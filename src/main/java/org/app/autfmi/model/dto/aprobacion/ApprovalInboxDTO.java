package org.app.autfmi.model.dto.aprobacion;

import java.util.List;

import org.app.autfmi.model.response.PaginatedResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalInboxDTO {
  private PaginatedResponse<ApprovalItemDTO> solicitudes;
  private List<ApprovalClientDTO> clientes;
}
