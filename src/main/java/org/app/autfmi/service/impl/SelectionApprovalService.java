package org.app.autfmi.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.request.*;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.RequirementRepository;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/** Finalizar Selección solicita aprobación; todavía no crea contratos ni ingresos. */
@Service
@RequiredArgsConstructor
public class SelectionApprovalService {
    private final RequirementRepository repository;
    private final ApprovalWorkflowService workflow;
    private final JdbcTemplate jdbc;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;
    @Value("${approval.flow.enabled:true}")
    private boolean enabled;

    public boolean enabled() { return enabled; }

    @Transactional(rollbackFor = Exception.class)
    public BaseResponse submit(UserDTO user, RequirementTalentRequest request) throws com.microsoft.sqlserver.jdbc.SQLServerException {
        if (request.getLstTalentos() == null) return new BaseResponse(1, "Faltan los talentos de la selección.");
        var context = jdbc.queryForList("SELECT R.ID_CLIENTE, R.CLIENTE FROM dbo.REQUERIMIENTO R "
                + "JOIN dbo.CLIENTE C ON C.ID_CLIENTE=R.ID_CLIENTE "
                + "WHERE R.ID_REQUERIMIENTO=? AND R.ID_ESTADO_REGISTRO=1 "
                + "AND C.ID_EMPRESA=? AND C.ID_ESTADO_REGISTRO=1", request.getIdRequerimiento(), user.getIdEmpresa());
        if (context.size() != 1) return new BaseResponse(1, "No se pudo identificar el cliente del requerimiento.");
        int client = ((Number) context.get(0).get("ID_CLIENTE")).intValue();
        String clientName = (String) context.get(0).get("CLIENTE");
        List<RequirementTalentRequestDTO> saved = new ArrayList<>();
        List<EmployeeEntryRequest> entries = new ArrayList<>();
        for (var talent : request.getLstTalentos()) {
            var copy = mapper.convertValue(talent, RequirementTalentRequestDTO.class);
            copy.setIdCliente(client);
            copy.setCliente(clientName);
            var registered = jdbc.queryForList("SELECT INGRESO FROM dbo.REQUERIMIENTO_TALENTO "
                    + "WHERE ID_REQUERIMIENTO=? AND ID_TALENTO=? AND ID_ESTADO_REGISTRO=1",
                    request.getIdRequerimiento(), copy.getIdTalento());
            boolean entered = registered.stream().anyMatch(row -> row.get("INGRESO") instanceof Number n && n.intValue()==1);
            copy.setIngreso(entered ? 1 : 0); // La BDD manda; clientes antiguos envían 1 al confirmar.
            if (copy.isConfirmado() && !entered
                    && !Integer.valueOf(0).equals(copy.getIdEstadoRegistro())) {
                copy.setIdEstado(2); // Datos completos; el estado de ingresado espera al gestor.
                entries.add(entry(request.getIdRequerimiento(), copy));
            }
            saved.add(copy);
        }
        var response = repository.saveRequirementTalents(new RequirementTalentRequest(
                request.getIdRequerimiento(), false, false, saved), Common.createBaseRequest(user, Constante.GUARDAR_REQUERIMIENTO));
        if (response.getBaseResponse().getIdTipoMensaje() != 2) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return response.getBaseResponse();
        }
        List<String> results = new ArrayList<>();
        for (var entry : entries) {
            BaseResponse result = workflow.submitEntry(user, entry);
            if (result.getIdTipoMensaje() != 2) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return result;
            }
            results.add(result.getMensaje());
        }
        return new BaseResponse(2, entries.isEmpty() ? "Selección guardada; no hay nuevos ingresos pendientes."
                : String.join(" ", results));
    }

    static EmployeeEntryRequest entry(Integer requirement, RequirementTalentRequestDTO talent) {
        EmployeeEntryRequest form = new EmployeeEntryRequest();
        BeanUtils.copyProperties(talent, form);
        form.setIdRequerimiento(requirement);
        form.setSeleccion(talent);
        form.setMontoBase(amount(talent.getMontoBase()));
        form.setMontoMovilidad(amount(talent.getMontoMovilidad()));
        form.setMontoTrimestral(amount(talent.getMontoTrimestral()));
        form.setMontoSemestral(amount(talent.getMontoSemestral()));
        return form;
    }

    private static Double amount(BigDecimal value) { return value == null ? 0d : value.doubleValue(); }
}
