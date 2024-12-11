package org.app.autfmi.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TalentRequest {
    private Integer idEmpresa;
    private Integer idTalento;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String email;
    private String dni;
    private Integer tiempoContrato;
    private Integer idTiempoContrato;
    private String fechaInicioLabores;
    private String cargo;
    private Double remuneracion;
    private Integer idMoneda;
    private Integer idModalidad;
    private String ubicacion;
}
