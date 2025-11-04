package org.app.autfmi.model.dto;

import lombok.Data;

@Data
public class GestorDTO {
  private String signature;
  private String fullname;

  public GestorDTO(String signature, String fullname) {
    this.signature = signature == null ? fullname : signature;
    this.fullname = fullname;
  }
}