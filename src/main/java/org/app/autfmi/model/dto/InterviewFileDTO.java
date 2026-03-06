package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewFileDTO {
  private Integer id;
  private String name;
  private String size;
  private String date;
  private String type;
}
