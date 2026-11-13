package org.app.autfmi.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.report.SelectionEntryReport;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.InicioPreferenceRepository;
import org.app.autfmi.repository.RequirementRepository;
import org.app.autfmi.util.Constante;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Ejecuta una única selección aprobada y su preferencia en la misma transacción. */
@Component
@RequiredArgsConstructor
public class SelectionEntryExecutor {
    private final RequirementRepository repository;
    private final InicioPreferenceRepository preference;
    private final JdbcTemplate jdbc;

    @Transactional(rollbackFor = Exception.class)
    public EmployeeOperationExecutor.Execution execute(BaseRequest actor, EmployeeEntryRequest form) throws com.microsoft.sqlserver.jdbc.SQLServerException {
        var rows = jdbc.queryForList("SELECT RT.INGRESO, R.CODIGO_RQ, R.CLIENTE FROM dbo.REQUERIMIENTO_TALENTO RT WITH (UPDLOCK,HOLDLOCK) "
                + "JOIN dbo.REQUERIMIENTO R ON R.ID_REQUERIMIENTO=RT.ID_REQUERIMIENTO "
                + "WHERE RT.ID_REQUERIMIENTO=? AND RT.ID_TALENTO=? AND RT.ID_ESTADO_REGISTRO=1 "
                + "AND RT.CONFIRMADO=1 AND R.ID_CLIENTE=? AND R.ID_ESTADO_REGISTRO=1",
                form.getIdRequerimiento(), form.getIdTalento(), form.getIdCliente());
        if (rows.size() != 1 || flag(rows.get(0).get("INGRESO"))) {
            return new EmployeeOperationExecutor.Execution(new BaseResponse(1,
                    selectionConflict(actor, form, rows)), null);
        }
        var talent = form.getSeleccion();
        if (form.getIdMoneda() == null) form.setIdMoneda(talent.getIdMoneda());
        if (form.getDeclararSunat() == null) form.setDeclararSunat(talent.getDeclararSunat());
        if (form.getSedeDeclarar() == null) form.setSedeDeclarar(talent.getSedeDeclarar());
        if (Integer.valueOf(2).equals(form.getDeclararSunat())) form.setSedeDeclarar("");
        BeanUtils.copyProperties(form, talent);
        if (form.getCorreoPersonal() != null) talent.setEmail(form.getCorreoPersonal());
        if (form.getCelularPersonal() != null) talent.setCelular(form.getCelularPersonal());
        talent.setMontoBase(amount(form.getMontoBase()));
        talent.setMontoMovilidad(amount(form.getMontoMovilidad()));
        talent.setMontoTrimestral(amount(form.getMontoTrimestral()));
        talent.setMontoSemestral(amount(form.getMontoSemestral()));
        talent.setConfirmado(true);
        talent.setIdEstado(4);
        talent.setIngreso(0);
        var base = new BaseRequest(actor.getIdUsuario(), actor.getUsername(), actor.getIdEmpresa(),
                actor.getIdRol(), Constante.GUARDAR_REQUERIMIENTO);
        var result = repository.saveRequirementTalents(new RequirementTalentRequest(
                form.getIdRequerimiento(), false, true, List.of(talent)), base);
        if (result.getBaseResponse().getIdTipoMensaje() != 2) {
            throw new IllegalStateException(result.getBaseResponse().getMensaje());
        }
        if (result.getReportesIngreso() == null || result.getReportesIngreso().size() != 1
                || !form.getIdTalento().equals(result.getReportesIngreso().get(0).getIdTalento())) {
            throw new IllegalStateException("No se confirmó el registro del ingreso aprobado.");
        }
        preference.apply(form);
        return new EmployeeOperationExecutor.Execution(result.getBaseResponse(),
                new SelectionEntryReport(result.getBaseResponse(), result, base));
    }

    private String selectionConflict(BaseRequest actor, EmployeeEntryRequest form, List<Map<String, Object>> rows) {
        String original = "requerimiento " + form.getIdRequerimiento();
        if (rows.size() == 1) {
            return "El ingreso ya se registró en " + requirementLabel(rows.get(0), form.getIdRequerimiento())
                    + ". Revisa el ingreso en Selección antes de volver a enviar.";
        }
        if (rows.size() > 1) {
            return "Hay más de una selección activa y confirmada para el " + original
                    + ". Pide a Selección que revise los registros duplicados.";
        }
        var requirements = jdbc.queryForList("""
                SELECT R.ID_REQUERIMIENTO, R.CODIGO_RQ, R.CLIENTE
                FROM dbo.REQUERIMIENTO R
                JOIN dbo.CLIENTE C ON C.ID_CLIENTE=R.ID_CLIENTE AND C.ID_EMPRESA=?
                WHERE R.ID_REQUERIMIENTO=? AND R.ID_CLIENTE=? AND R.ID_ESTADO_REGISTRO=1
                """, actor.getIdEmpresa(), form.getIdRequerimiento(), form.getIdCliente());
        if (requirements.isEmpty()) {
            return "El " + original + " ya no está activo para el cliente de esta solicitud. Pide a Selección que confirme el requerimiento correcto.";
        }
        String label = requirementLabel(requirements.get(0), form.getIdRequerimiento());
        var originalRows = jdbc.queryForList("""
                SELECT CONFIRMADO, INGRESO FROM dbo.REQUERIMIENTO_TALENTO
                WHERE ID_REQUERIMIENTO=? AND ID_TALENTO=? AND ID_ESTADO_REGISTRO=1
                """, form.getIdRequerimiento(), form.getIdTalento());
        if (originalRows.stream().anyMatch(row -> flag(row.get("INGRESO")))) {
            return "El ingreso ya figura registrado en " + label + ". Revisa el ingreso en Selección antes de volver a enviar.";
        }
        var other = jdbc.queryForList("""
                SELECT TOP (3) R.ID_REQUERIMIENTO, R.CODIGO_RQ, R.CLIENTE, MAX(CAST(RT.INGRESO AS INT)) AS INGRESO
                FROM dbo.REQUERIMIENTO_TALENTO RT
                JOIN dbo.REQUERIMIENTO R ON R.ID_REQUERIMIENTO=RT.ID_REQUERIMIENTO
                WHERE RT.ID_TALENTO=? AND RT.ID_REQUERIMIENTO<>? AND R.ID_CLIENTE=?
                  AND RT.ID_ESTADO_REGISTRO=1 AND R.ID_ESTADO_REGISTRO=1
                  AND (RT.CONFIRMADO=1 OR RT.INGRESO=1)
                GROUP BY R.ID_REQUERIMIENTO, R.CODIGO_RQ, R.CLIENTE
                ORDER BY R.ID_REQUERIMIENTO DESC
                """, form.getIdTalento(), form.getIdRequerimiento(), form.getIdCliente());
        String reason = originalRows.isEmpty()
                ? "El talento ya no figura con una selección activa en " + label + "."
                : "El talento ya no figura como confirmado en " + label + ".";
        if (!other.isEmpty()) {
            var matches = other.stream().map(row -> requirementLabel(row, row.get("ID_REQUERIMIENTO"))
                    + (flag(row.get("INGRESO")) ? " (ingreso registrado)" : " (selección confirmada)")).toList();
            reason += " Otros requerimientos del mismo cliente: " + String.join(", ", matches) + ".";
        }
        return reason + " Pide a Selección que confirme cuál es el requerimiento correcto.";
    }

    private static String requirementLabel(Map<String, Object> row, Object id) {
        Object code = row.get("CODIGO_RQ");
        Object client = row.get("CLIENTE");
        String label = code == null || code.toString().isBlank() ? "RQ " + id : code.toString();
        return client == null || client.toString().isBlank() ? label : label + " · " + client;
    }

    private static boolean flag(Object value) {
        return Boolean.TRUE.equals(value) || (value instanceof Number number && number.intValue() == 1);
    }

    private static BigDecimal amount(Double value) { return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value); }
}
