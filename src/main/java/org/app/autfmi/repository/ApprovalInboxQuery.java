package org.app.autfmi.repository;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.ApprovalListRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** Parameterized read query: identical permission scope for counts and page rows. */
final class ApprovalInboxQuery {
  private final NamedParameterJdbcTemplate jdbc;
  ApprovalInboxQuery(JdbcTemplate template) { jdbc = new NamedParameterJdbcTemplate(template); }
  static final String CLIENTS = "dbo.FN_FMI_BANDEJA_CLIENTES_PERMITIDOS(:usuario, :empresa, :roles)";
  static final String SCOPE = """
      FROM dbo.FMI_SOLICITUD_APROBACION S
      JOIN dbo.FN_FMI_BANDEJA_CLIENTES_PERMITIDOS(:usuario, :empresa, :roles) C ON C.ID_CLIENTE=S.ID_CLIENTE
      JOIN dbo.USUARIOS U ON U.ID_USUARIO=S.ID_USUARIO_SOLICITANTE AND U.ID_EMPRESA=:empresa
      WHERE S.ID_ESTADO_REGISTRO=1
      AND EXISTS (SELECT 1 FROM dbo.FN_FMI_1124_SOLICITUDES_VISIBLES(:usuario,:empresa,:roles) V WHERE V.ID_SOLICITUD=S.ID_SOLICITUD)
      AND (:cliente IS NULL OR S.ID_CLIENTE=:cliente)
      AND (:tipo IS NULL OR S.TIPO_SOLICITUD=:tipo)
      AND (:estado IS NULL OR S.ESTADO=:estado)
      AND (:desde IS NULL OR S.FCHCRE>=CAST(:desde AS date))
      AND (:hasta IS NULL OR S.FCHCRE<DATEADD(DAY,1,CAST(:hasta AS date)))
      AND (:busqueda IS NULL OR S.ID_SOLICITUD=:numero
           OR S.NOMBRE_TALENTO COLLATE Latin1_General_CI_AI LIKE :patron ESCAPE N'\\'
           OR S.RESUMEN COLLATE Latin1_General_CI_AI LIKE :patron ESCAPE N'\\')
      """;
  Map<String,Object> list(ApprovalListRequest f, UserDTO u, String roles) {
    String search=f.getBusqueda();
    String pattern=search==null?null:"%"+search.replace("\\","\\\\").replace("%","\\%").replace("_","\\_").replace("[","\\[")+"%";
    Integer number=null;
    if(search!=null && search.matches("[0-9]+")) { try { number=Integer.valueOf(search); } catch(NumberFormatException ignored) { } }
    MapSqlParameterSource p=new MapSqlParameterSource()
        .addValue("usuario",u.getIdUsuario(),Types.INTEGER).addValue("empresa",u.getIdEmpresa(),Types.INTEGER)
        .addValue("roles",roles,Types.VARCHAR).addValue("cliente",f.getIdCliente(),Types.INTEGER)
        .addValue("tipo",f.getTipoSolicitud(),Types.VARCHAR).addValue("estado",f.getEstado(),Types.VARCHAR)
        .addValue("desde",blank(f.getFechaDesde()),Types.VARCHAR).addValue("hasta",blank(f.getFechaHasta()),Types.VARCHAR)
        .addValue("busqueda",search,Types.NVARCHAR).addValue("numero",number,Types.INTEGER).addValue("patron",pattern,Types.NVARCHAR)
        .addValue("offset",(long)(f.getNPag()-1)*f.getCPag(),Types.BIGINT).addValue("cantidad",f.getCPag(),Types.INTEGER);
    Integer access=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.FN_FMI_BANDEJA_ACCESO(:usuario,:empresa,:roles)",p,Integer.class);
    if(access==null || access==0) return Map.of("#result-set-1",List.of(Map.of("CODIGO_RESULTADO","SIN_PERMISO","ID_TIPO_MENSAJE",1,"MENSAJE","No tienes permiso para ver la bandeja de aprobaciones.")));
    Integer total=jdbc.queryForObject("SELECT COUNT(*) "+SCOPE,p,Integer.class);
    List<Map<String,Object>> rows=jdbc.queryForList("""
        SELECT S.ID_SOLICITUD,S.TIPO_SOLICITUD,S.ESTADO,S.ID_CLIENTE,C.RAZON_SOCIAL AS CLIENTE,
        S.ID_TALENTO,S.NOMBRE_TALENTO,S.RESUMEN,U.USUARIO AS USUARIO_SOLICITANTE,
        CONVERT(VARCHAR(19),S.FCHCRE,120) AS FECHA_SOLICITUD,
        CASE WHEN S.ESTADO<>'PENDIENTE' THEN (SELECT CONVERT(VARCHAR(19),MAX(H.FCHCRE),120)
        FROM dbo.FMI_SOLICITUD_APROBACION_HIST H WHERE H.ID_SOLICITUD=S.ID_SOLICITUD
        AND H.ACCION IN ('APROBADA','RESUELTA') AND H.ESTADO_NUEVO=S.ESTADO) END AS FECHA_RESOLUCION
        """+SCOPE+" ORDER BY S.FCHCRE DESC,S.ID_SOLICITUD DESC OFFSET :offset ROWS FETCH NEXT :cantidad ROWS ONLY",p);
    List<Map<String,Object>> clients=jdbc.queryForList("SELECT ID_CLIENTE,RAZON_SOCIAL FROM "+CLIENTS+" ORDER BY RAZON_SOCIAL",p);
    return Map.of("#result-set-1",List.of(Map.of("CODIGO_RESULTADO","OK","ID_TIPO_MENSAJE",2,"MENSAJE","Consulta realizada correctamente.","TOTAL_ELEMENTOS",total,"TOTAL_PAGINAS",(int)Math.ceil(total/(double)f.getCPag()))),"#result-set-2",rows,"#result-set-3",clients);
  }
  private static String blank(String value) { return value==null||value.isBlank()?null:value; }
}
