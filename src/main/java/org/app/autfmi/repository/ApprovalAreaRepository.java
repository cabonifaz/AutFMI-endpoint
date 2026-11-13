package org.app.autfmi.repository;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.dto.aprobacion.*;
import org.app.autfmi.util.ApprovalAreaPolicy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ApprovalAreaRepository {

  private final JdbcTemplate jdbc;

  public Long lock(Integer id) {
    var rows = jdbc.queryForList(
      "SELECT CONVERT(BIGINT,VERSION_FILA) VERSION FROM dbo.FMI_SOLICITUD_APROBACION WITH(UPDLOCK,HOLDLOCK) WHERE ID_SOLICITUD=? AND ID_ESTADO_REGISTRO=1",
      id
    );
    return rows.size() == 1
      ? ((Number) rows.get(0).get("VERSION")).longValue()
      : null;
  }

  public Map<String, Map<String, Object>> states(Integer id) {
    Map<String, Map<String, Object>> result = new LinkedHashMap<>();
    for (var row : jdbc.queryForList(
      "SELECT AREA,ESTADO,USUARIO,CONVERT(VARCHAR(19),FCHRESOLUCION,120) FECHA FROM dbo.FMI_SOLICITUD_APROBACION_AREA WHERE ID_SOLICITUD=?",
      id
    ))
      result.put((String) row.get("AREA"), row);
    return result;
  }

  public List<String> reviewed(Integer id, UserDTO user, Long version) {
    return jdbc.queryForList(
      "SELECT AREA FROM dbo.FMI_SOLICITUD_APROBACION_REVISION WHERE ID_SOLICITUD=? AND ID_USUARIO=? AND VERSION=?",
      String.class,
      id,
      user.getIdUsuario(),
      version
    );
  }

  public void review(Integer id, UserDTO user, Long version, String area) {
    jdbc.update(
      "DELETE FROM dbo.FMI_SOLICITUD_APROBACION_REVISION WHERE ID_SOLICITUD=? AND ID_USUARIO=? AND AREA=?",
      id,
      user.getIdUsuario(),
      area
    );
    jdbc.update(
      "INSERT dbo.FMI_SOLICITUD_APROBACION_REVISION(ID_SOLICITUD,ID_USUARIO,AREA,VERSION) VALUES(?,?,?,?)",
      id,
      user.getIdUsuario(),
      area,
      version
    );
  }

  public void initialize(Integer id, List<ApprovalAreaDTO> areas) {
    for (var a : areas)
      jdbc.update(
        "IF NOT EXISTS(SELECT 1 FROM dbo.FMI_SOLICITUD_APROBACION_AREA WHERE ID_SOLICITUD=? AND AREA=?) INSERT dbo.FMI_SOLICITUD_APROBACION_AREA(ID_SOLICITUD,AREA,ESTADO) VALUES(?,?,?)",
        id,
        a.area(),
        id,
        a.area(),
        a.estado()
      );
  }

  public String equipmentType(int id) {
    return jdbc.queryForList("""
      SELECT STRING1 FROM dbo.PARAMETROS
      WHERE ID_MAESTRO=21 AND NUM1=? AND ID_ESTADO_REGISTRO=1
      """, String.class, id).stream().filter(Objects::nonNull).findFirst().orElse(null);
  }

  public void syncEquipment(Integer id, boolean applies) {
    jdbc.update("""
      UPDATE dbo.FMI_SOLICITUD_APROBACION_AREA SET ESTADO=?
      WHERE ID_SOLICITUD=? AND AREA='EQUIPO' AND ESTADO IN ('PENDIENTE', 'NO_APLICA')
      """, applies ? "PENDIENTE" : "NO_APLICA", id);
  }

  public List<org.app.autfmi.model.request.SolicitudSoftwareRequest> defaultSoftware() {
    return jdbc.query("""
      SELECT STRING1, STRING2 FROM dbo.PARAMETROS
      WHERE ID_MAESTRO=23 AND ID_ESTADO_REGISTRO=1 AND NULLIF(LTRIM(RTRIM(STRING1)), '') IS NOT NULL
      ORDER BY NUM1
      """, (row, index) -> new org.app.autfmi.model.request.SolicitudSoftwareRequest(
        index + 1, row.getString("STRING1"), row.getString("STRING2")));
  }

  public void rejectAll(Integer id, UserDTO user, String reason) {
    jdbc.update("""
      UPDATE dbo.FMI_SOLICITUD_APROBACION_AREA SET ESTADO='RECHAZADA', USUARIO=?, FCHRESOLUCION=GETDATE()
      WHERE ID_SOLICITUD=? AND ESTADO IN ('PENDIENTE','APROBADA')
      """, user.getUsuario(), id);
    audit(id, "AREA_RECHAZADA", null, "RECHAZADA", "Desaprobación total: " + reason, user);
  }

  public void resolve(
    Integer id,
    String area,
    String state,
    UserDTO user,
    String reason
  ) {
    int changed = jdbc.update(
      "UPDATE dbo.FMI_SOLICITUD_APROBACION_AREA SET ESTADO=?,USUARIO=?,FCHRESOLUCION=GETDATE() WHERE ID_SOLICITUD=? AND AREA=? AND ESTADO='PENDIENTE'",
      state,
      user.getUsuario(),
      id,
      area
    );
    if (changed != 1) throw new IllegalStateException(
      "El área ya fue resuelta. Recargue la solicitud."
    );
    audit(
      id,
      "APROBADA".equals(state) ? "AREA_APROBADA" : "AREA_RECHAZADA",
      "PENDIENTE",
      "PENDIENTE",
      area + (reason == null ? "" : ": " + reason),
      user
    );
  }

  public ApprovalDecisionDTO finish(Integer id, UserDTO user) {
    var rows = jdbc.queryForList(
      "SELECT ESTADO FROM dbo.FMI_SOLICITUD_APROBACION_AREA WHERE ID_SOLICITUD=?",
      String.class,
      id
    );
    String state = rows.contains("PENDIENTE")
      ? "PENDIENTE"
      : rows.contains("RECHAZADA")
        ? "RECHAZADA"
        : "APROBADA";
    jdbc.update(
      "UPDATE dbo.FMI_SOLICITUD_APROBACION SET ESTADO=?,USUMOD=?,FCHMOD=GETDATE() WHERE ID_SOLICITUD=?",
      state,
      user.getUsuario(),
      id
    );
    if (!"PENDIENTE".equals(state)) audit(
      id,
      "RESUELTA",
      "PENDIENTE",
      state,
      "Todas las áreas aplicables fueron resueltas.",
      user
    );
    return new ApprovalDecisionDTO(id, state, lock(id));
  }

  public ApprovalDecisionDTO cancel(Integer id, UserDTO user, String reason) {
    int changed = jdbc.update("""
      UPDATE dbo.FMI_SOLICITUD_APROBACION SET ESTADO='ANULADA', USUMOD=?, FCHMOD=GETDATE()
      WHERE ID_SOLICITUD=? AND ESTADO='PENDIENTE'
      """, user.getUsuario(), id);
    if (changed != 1) throw new IllegalStateException("La solicitud ya no está pendiente.");
    audit(id, "RESUELTA", "PENDIENTE", "ANULADA", reason, user);
    return new ApprovalDecisionDTO(id, "ANULADA", lock(id));
  }

  public void update(Integer id, String json, UserDTO user, String area) {
    jdbc.update(
      "UPDATE dbo.FMI_SOLICITUD_APROBACION SET DATOS_FORMULARIO=?,USUMOD=?,FCHMOD=GETDATE() WHERE ID_SOLICITUD=?",
      json,
      user.getUsuario(),
      id
    );
    audit(
      id,
      "FORMULARIO_EDITADO",
      "PENDIENTE",
      "PENDIENTE",
      "Corrección del área " + area,
      user
    );
  }

  public void audit(
    Integer id,
    String action,
    String before,
    String after,
    String detail,
    UserDTO user
  ) {
    jdbc.update(
      "INSERT dbo.FMI_SOLICITUD_APROBACION_HIST(ID_SOLICITUD,ACCION,ESTADO_ANTERIOR,ESTADO_NUEVO,DETALLE,ID_USUARIO,USUARIO,FCHCRE) VALUES(?,?,?,?,?,?,?,GETDATE())",
      id,
      action,
      before,
      after,
      detail,
      user.getIdUsuario(),
      user.getUsuario()
    );
  }

  public List<ApprovalGestorDTO> managers(Integer client, Integer company) {
    return jdbc.query(
      "SELECT U.ID_USUARIO,U.USUARIO,MIN(UR.ID_TIPO_ROL) ROL FROM dbo.CLIENTE_GESTOR C JOIN dbo.USUARIOS U ON U.ID_USUARIO=C.ID_USUARIO JOIN dbo.USUARIO_ROL UR ON UR.ID_USUARIO=U.ID_USUARIO AND UR.ID_ESTADO_REGISTRO=1 WHERE C.ID_CLIENTE=? AND C.ID_ESTADO_REGISTRO=1 AND U.ID_EMPRESA=? AND U.ID_ESTADO_REGISTRO=1 GROUP BY U.ID_USUARIO,U.USUARIO",
      (rs, n) ->
        new ApprovalGestorDTO(rs.getInt(1), rs.getString(2), rs.getInt(3)),
      client,
      company
    );
  }

  public ApprovalGestorDTO applicant(Integer id, Integer company) {
    var rows = jdbc.query(
      "SELECT U.ID_USUARIO,U.USUARIO,MIN(UR.ID_TIPO_ROL) ROL FROM dbo.FMI_SOLICITUD_APROBACION S JOIN dbo.USUARIOS U ON U.ID_USUARIO=S.ID_USUARIO_SOLICITANTE JOIN dbo.USUARIO_ROL UR ON UR.ID_USUARIO=U.ID_USUARIO AND UR.ID_ESTADO_REGISTRO=1 WHERE S.ID_SOLICITUD=? AND U.ID_EMPRESA=? AND U.ID_ESTADO_REGISTRO=1 GROUP BY U.ID_USUARIO,U.USUARIO",
      (rs, n) ->
        new ApprovalGestorDTO(rs.getInt(1), rs.getString(2), rs.getInt(3)),
      id,
      company
    );
    return rows.isEmpty() ? null : rows.get(0);
  }
}
