package org.app.autfmi.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fila devuelta por SP_BT_NOTIF_INICIO_LST: un contrato de outsourcing pendiente
 * de notificar su inicio de labores.
 *
 * <ul>
 *   <li>{@code tipoHito}: "D2" (faltan 2 días) o "D0" (día de inicio).</li>
 *   <li>{@code nuevoEstado}: valor con el que se marca el contrato al enviar
 *       (1 para D2, 2 para D0).</li>
 *   <li>{@code correosCliente}: CSV de contactos del requerimiento (TO).</li>
 *   <li>{@code correosGestores}: CSV de gestores del cliente (CC).</li>
 * </ul>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InicioOutsourcingDTO {
  private int idContrato;
  private String tipoHito;
  private int nuevoEstado;
  private String nombres;
  private String apellidos;
  private String dni;
  private String celular;
  private String email;
  private String cargo;
  private String fchInicio;
  private String modalidad;
  private String cliente;
  private String correosCliente;
  private String correosGestores;
}
