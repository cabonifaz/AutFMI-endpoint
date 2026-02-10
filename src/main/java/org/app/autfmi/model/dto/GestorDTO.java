package org.app.autfmi.model.dto;

import lombok.Data;

@Data
public class GestorDTO {
  private String signature;
  private String fullname;

  public GestorDTO(String signature, String fullname) {
    this.signature = signature;
    this.fullname = fullname;
  }
}