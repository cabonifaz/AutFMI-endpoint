package org.app.autfmi.service.impl;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.EmployeeContractEndRequest;
import org.app.autfmi.model.request.EmployeeEntryRequest;
import org.app.autfmi.model.request.EmployeeMovementRequest;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.repository.EmployeeRepository;
import org.app.autfmi.repository.HistoryRepository;
import org.app.autfmi.service.IEmployeeService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.JwtHelper;
import org.app.autfmi.util.PDFUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeService implements IEmployeeService {
    private final EmployeeRepository employeeRepository;
    private final HistoryRepository historyRepository;
    private final PDFUtils pdfUtils;
    private final JwtHelper jwt;

    @Override
    public BaseResponse getEmployee(Integer idUsuarioTalento) {
        return employeeRepository.getEmployee(idUsuarioTalento);
    }

    @Override
    public BaseResponse saveEmployeeEntry(String token, EmployeeEntryRequest request) throws MessagingException {
        UserDTO user = jwt.decodeToken(token);
        String funcionalidades = String.join(",", Constante.GUARDAR_USUARIO, Constante.REALIZAR_INGRESO);
        BaseRequest baseRequest = Common.createBaseRequest(user, funcionalidades);
        EntryReport report = employeeRepository.saveEmployeeEntry(baseRequest, request);

        if (report != null && report.getResponse().getIdTipoMensaje() == 2) {
            String template = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.MOVIMIENTO);
            pdfUtils.enviarCorreoConPDF(
                    pdfUtils.replaceEntryRequestValues(template, report),
                    report.getCorreoGestor(),
                    "Ingreso empleado",
                    "Formulario de nuevo ingreso de empleado."
            );
        }

        return report.getResponse();
    }

    @Override
    public BaseResponse saveEmployeeMovement(String token, EmployeeMovementRequest request) throws MessagingException {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.REALIZAR_MOVIMIENTO);
        MovementReport report = historyRepository.registerMovement(baseRequest, request);

        if (report != null && report.getResponse().getIdTipoMensaje() == 2) {
            String template = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.MOVIMIENTO);
            pdfUtils.enviarCorreoConPDF(
                    pdfUtils.replaceMovementRequestValues(template, report),
                    report.getCorreoGestor(),
                    "Ingreso empleado",
                    "Formulario de nuevo ingreso de empleado."
            );
        }

        return report.getResponse();
    }

    @Override
    public BaseResponse saveEmployeeContractEnd(String token, EmployeeContractEndRequest request) throws MessagingException {
        UserDTO user = jwt.decodeToken(token);
        BaseRequest baseRequest = Common.createBaseRequest(user, Constante.REALIZAR_CESE);
        CeseReport report = historyRepository.registerContractTermination(baseRequest, request);

        if (report != null && report.getResponse().getIdTipoMensaje() == 2) {
            String template = pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.MOVIMIENTO);
            pdfUtils.enviarCorreoConPDF(
                    pdfUtils.replaceOutRequestValues(template, report),
                    report.getCorreoGestor(),
                    "Ingreso empleado",
                    "Formulario de nuevo ingreso de empleado."
            );
        }

        return report.getResponse();
    }
}
