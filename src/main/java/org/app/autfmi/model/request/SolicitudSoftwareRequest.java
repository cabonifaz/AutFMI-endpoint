package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolicitudSoftwareRequest {
    private Integer idItem;
    private String producto;
    private String prodVersion;
}
