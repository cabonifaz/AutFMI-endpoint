package org.app.autfmi.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
  private List<T> items;
  private Integer totalElements;
  private Integer totalPages;
  private Integer currentPage;
}