package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParametrosDTO {
    private Integer idParametro;
    private Integer idMaestro;
    private Integer num1;
    private Integer num2;
    private Integer num3;
    private String string1;
    private String string2;
    private String string3;
}
