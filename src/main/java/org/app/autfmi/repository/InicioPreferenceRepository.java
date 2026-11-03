package org.app.autfmi.repository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Preferencia por contrato; NULL y 2 conservan su comportamiento histórico. */
@Repository
@RequiredArgsConstructor
public class InicioPreferenceRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final String CONTRATO = " FROM dbo.TALENTO_CONTRATO TC "
            + "WHERE TC.ID_TALENTO = ? AND TC.ID_RQ = ? AND TC.ID_ESTADO_REGISTRO = 1 AND TC.ACTIVO = 1 "
            + "AND EXISTS (SELECT 1 FROM dbo.PARAMETROS P WHERE P.ID_MAESTRO = 7 "
            + "AND P.NUM1 = TC.ID_AREA AND P.STRING1 = 'Outsourcing' AND P.ID_ESTADO_REGISTRO = 1)";

    public record Preference(boolean disponible, int notificar) { }

    public Preference get(EmployeeEntryRequest request) {
        List<Integer> estados = jdbcTemplate.query("SELECT TC.ESTADO_NOTIF_INICIO" + CONTRATO,
                (rs, row) -> (Integer) rs.getObject(1), request.getIdTalento(), request.getIdRequerimiento());
        // La selección nueva aún no crea contrato: el gestor decide antes de aprobar.
        if (estados.isEmpty() && request.getSeleccion() != null) {
            Integer pending = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.REQUERIMIENTO_TALENTO RT "
                    + "JOIN dbo.REQUERIMIENTO R ON R.ID_REQUERIMIENTO=RT.ID_REQUERIMIENTO "
                    + "WHERE RT.ID_TALENTO=? AND RT.ID_REQUERIMIENTO=? AND RT.ID_ESTADO_REGISTRO=1 "
                    + "AND RT.CONFIRMADO=1 AND ISNULL(RT.INGRESO,0)=0 AND R.ID_CLIENTE=? "
                    + "AND R.ID_ESTADO_REGISTRO=1 AND EXISTS(SELECT 1 FROM dbo.PARAMETROS P "
                    + "WHERE P.ID_MAESTRO=7 AND P.NUM1=? AND P.STRING1='Outsourcing' AND P.ID_ESTADO_REGISTRO=1)",
                    Integer.class, request.getIdTalento(), request.getIdRequerimiento(), request.getIdCliente(), request.getIdArea());
            return new Preference(Integer.valueOf(1).equals(pending), Integer.valueOf(1).equals(pending) ? (Integer.valueOf(0).equals(request.getNotificarProntoInicio()) ? 0 : 1) : 0);
        }
        if (estados.size() != 1 || estados.get(0) == null || !List.of(0, 1, 3).contains(estados.get(0))) {
            return new Preference(false, 0);
        }
        return new Preference(true, estados.get(0) == 3 ? 0 : 1);
    }

    public void apply(EmployeeEntryRequest request) {
        Integer notificar = request.getNotificarProntoInicio();
        if (notificar == null) return;
        if (notificar != 0 && notificar != 1) throw new IllegalArgumentException("Preaviso no válido.");
        if (!get(request).disponible()) throw new IllegalArgumentException("El preaviso no está disponible para este ingreso.");
        int updated = jdbcTemplate.update("UPDATE TC SET ESTADO_NOTIF_INICIO = CASE WHEN ? = 0 THEN 3 "
                + "WHEN TC.ESTADO_NOTIF_INICIO = 3 THEN 0 ELSE TC.ESTADO_NOTIF_INICIO END" + CONTRATO
                + " AND TC.ESTADO_NOTIF_INICIO IN (0,1,3)",
                notificar, request.getIdTalento(), request.getIdRequerimiento());
        if (updated != 1) throw new IllegalStateException("El contrato cambió; actualice la solicitud.");
    }
}