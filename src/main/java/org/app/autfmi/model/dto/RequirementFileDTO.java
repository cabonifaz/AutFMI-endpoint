package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementFileDTO {
    private Integer idRequerimientoArchivo;
    private String link;
    private String nombreArchivo;
    private Integer idTipoArchivo;
}
