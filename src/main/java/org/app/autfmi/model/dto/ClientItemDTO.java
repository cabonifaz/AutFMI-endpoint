package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientItemDTO {
    private Integer idCliente;
    private String razonSocial;
    private Integer total;
}
