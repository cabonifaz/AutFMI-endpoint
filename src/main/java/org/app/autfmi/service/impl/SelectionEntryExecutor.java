package org.app.autfmi.service.impl;

import java.math.BigDecimal;
import java.util.List;
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
        var rows = jdbc.queryForList("SELECT RT.INGRESO FROM dbo.REQUERIMIENTO_TALENTO RT WITH (UPDLOCK,HOLDLOCK) "
                + "JOIN dbo.REQUERIMIENTO R ON R.ID_REQUERIMIENTO=RT.ID_REQUERIMIENTO "
                + "WHERE RT.ID_REQUERIMIENTO=? AND RT.ID_TALENTO=? AND RT.ID_ESTADO_REGISTRO=1 "
                + "AND RT.CONFIRMADO=1 AND R.ID_CLIENTE=? AND R.ID_ESTADO_REGISTRO=1",
                form.getIdRequerimiento(), form.getIdTalento(), form.getIdCliente());
        if (rows.size() != 1 || Integer.valueOf(1).equals(rows.get(0).get("INGRESO"))) {
            return new EmployeeOperationExecutor.Execution(new BaseResponse(1,
                    "El talento ya ingresó o su selección cambió; revise el requerimiento."), null);
        }
        var talent = form.getSeleccion();
        BeanUtils.copyProperties(form, talent);
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

    private static BigDecimal amount(Double value) { return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value); }
}
