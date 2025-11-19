package org.app.autfmi.model.response;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TalentEmployeeList extends BaseResponse {

  private List<EmployeeItem> talentos;
  private Integer totalElementos;
  private Integer totalPaginas;

  public TalentEmployeeList(Integer messageId, String message) {
    super(messageId, message);
  }

  public TalentEmployeeList withMetadata(Integer totalPages, Integer totalElements) {
    this.setTotalElementos(totalElements);
    this.setTotalPaginas(totalPages);
    return this;
  }

  public TalentEmployeeList withTalents(List<EmployeeItem> employees) {
    this.setTalentos(employees);
    return this;
  }

  public record EmployeeItem(
      Integer idTalento,
      String nombres,
      String apellidos,
      Integer idActivo) {
  }

}
