package org.app.autfmi.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalDetailDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalInboxDTO;
import org.app.autfmi.model.dto.aprobacion.ApprovalOutcome;
import org.app.autfmi.model.dto.aprobacion.ApprovalResult;
import org.app.autfmi.model.request.ApprovalListRequest;
import org.app.autfmi.repository.ApprovalRepository;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class ApprovalService {

  private final ApprovalRepository approvalRepository;
  private final JwtHelper jwt;

  /** Token ausente, vencido, mal firmado o sin la identidad necesaria. */
  public static class InvalidSessionException extends RuntimeException {
    public InvalidSessionException(String message) {
      super(message);
    }
  }

  public ApprovalResult<ApprovalInboxDTO> listRequests(String token, ApprovalListRequest request) {
    UserDTO user = authenticatedUser(token);

    String tipo = normalize(request.getTipoSolicitud());
    String estado = normalize(request.getEstado());
    String busqueda = request.getBusqueda() == null || request.getBusqueda().isBlank()
        ? null : request.getBusqueda().trim();
    int nPag = request.getNPag() == null ? 1 : request.getNPag();
    int cPag = request.getCPag() == null ? Constante.TAMANIO_PAGINA_APROBACIONES : request.getCPag();

    if ((tipo != null && !Constante.TIPOS_SOLICITUD_APROBACION.contains(tipo))
        || (estado != null && !Constante.ESTADOS_SOLICITUD_APROBACION.contains(estado))
        || (request.getIdCliente() != null && request.getIdCliente() < 1)
        || (busqueda != null && busqueda.length() > Constante.MAX_LONGITUD_BUSQUEDA_APROBACIONES)
        || nPag < 1
        || cPag < 1 || cPag > Constante.MAX_TAMANIO_PAGINA_APROBACIONES) {
      return ApprovalResult.of(ApprovalOutcome.PARAMETRO_INVALIDO, 1, "Los filtros de la bandeja no son válidos.", null);
    }

    try {
      java.time.LocalDate desde = parseDate(request.getFechaDesde());
      java.time.LocalDate hasta = parseDate(request.getFechaHasta());
      if (desde != null && hasta != null && desde.isAfter(hasta)) throw new IllegalArgumentException();
    } catch (IllegalArgumentException | java.time.DateTimeException ex) {
      return ApprovalResult.of(ApprovalOutcome.PARAMETRO_INVALIDO, 1, "El rango de fechas no es válido. Usa fechas desde y hasta en formato yyyy-MM-dd.", null);
    }
    ApprovalListRequest filters = new ApprovalListRequest(request.getIdCliente(), tipo, estado, busqueda, nPag, cPag);
    filters.setFechaDesde(request.getFechaDesde());
    filters.setFechaHasta(request.getFechaHasta());
    return approvalRepository.listRequests(filters, user, rolesCsv(user));
  }

  public ApprovalResult<ApprovalDetailDTO> getRequest(String token, Integer idSolicitud) {
    UserDTO user = authenticatedUser(token);
    if (idSolicitud == null || idSolicitud < 1) {
      return ApprovalResult.of(ApprovalOutcome.PARAMETRO_INVALIDO, 1, "El identificador de la solicitud no es válido.", null);
    }
    return approvalRepository.getRequest(idSolicitud, user, rolesCsv(user));
  }

  /** Usuario del token, validado igual que en la bandeja. Lanza InvalidSessionException. */
  public UserDTO authenticate(String token) {
    return authenticatedUser(token);
  }

  private UserDTO authenticatedUser(String token) {
    if (token == null || token.isBlank()) {
      throw new InvalidSessionException("Sesión no iniciada");
    }
    UserDTO user;
    try {
      user = jwt.decodeToken(token);
    } catch (RuntimeException e) {
      // Vencido (ExpiredJwtException), firma inválida, mal formado o claims de otro tipo.
      throw new InvalidSessionException("Sesión vencida o inválida");
    }
    if (user.getIdUsuario() == null || user.getIdEmpresa() == null || rolesCsv(user).isEmpty()) {
      throw new InvalidSessionException("Sesión vencida o inválida");
    }
    return user;
  }

  /** Roles del token como "2,3". Solo enteros: el SP los coteja con USUARIO_ROL. */
  static String rolesCsv(UserDTO user) {
    List<?> roles = user.getIdRoles();
    if (roles == null) {
      return "";
    }
    return roles.stream()
        .filter(Objects::nonNull)
        .filter(Number.class::isInstance)
        .map(r -> String.valueOf(((Number) r).intValue()))
        .distinct()
        .collect(Collectors.joining(","));
  }

  private static java.time.LocalDate parseDate(String value) {
    if (value == null || value.isBlank()) return null;
    if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException();
    java.time.LocalDate date = java.time.LocalDate.parse(value);
    if (date.getYear() < 1753 || date.getYear() > 9998) throw new IllegalArgumentException();
    return date;
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
  }
}
