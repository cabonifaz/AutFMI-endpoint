package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO {
    public String nombreArchivo;
    public String htmlTemplate;
    public byte[] byteArchivo;
}
