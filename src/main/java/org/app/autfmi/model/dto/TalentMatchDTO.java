package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un posible "ya existe" al cargar un colaborador desde un FMI.
 *
 * Lleva DNI, correo, situación y en cuántos requerimientos figura porque el
 * formulario sólo aporta el nombre, y con el nombre solo no se distingue a dos
 * homónimos: es el operador quien decide mirando estos datos.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TalentMatchDTO {

    private Integer idTalento;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String dni;
    private String email;
    private String celular;

    /** LIBRE u OCUPADO (maestro 26). */
    private String situacion;

    /** dd/MM/yyyy: cuándo se dio de alta en el banco. */
    private String fchAlta;

    /** En cuántos requerimientos vigentes está. */
    private Integer rqs;

    /**
     * Qué tan seguro es el match: 100 mismo documento, 95 mismo correo, y por
     * nombre el porcentaje de palabras que coinciden.
     */
    private Integer score;
}
