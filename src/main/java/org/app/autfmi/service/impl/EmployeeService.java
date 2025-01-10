package org.app.autfmi.service.impl;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.app.autfmi.model.dto.FileDTO;
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

import java.util.ArrayList;
import java.util.List;

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
            FileDTO fileFormulario = new FileDTO(
                    "FT-GT-12 Formulario de Ingreso",
                    pdfUtils.replaceEntryRequestValues(pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO),report),
                    null
            );

            FileDTO fileSolicitud = new FileDTO(
                    "FT-GS-01 Solicitud de Creación de Usuario",
                    pdfUtils.replaceEntryRequestValues(pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD),report),
                    null
            );

            List<FileDTO> lstfiles = new ArrayList<>();
            lstfiles.add(fileFormulario);
            lstfiles.add(fileSolicitud);

            pdfUtils.enviarCorreoConPDF(
                    lstfiles,
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
            List<FileDTO> lstfiles = new ArrayList<>();
            FileDTO fileFormulario = new FileDTO(
                    "FT-GT-12 Formulario de Movimiento",
                    pdfUtils.replaceMovementRequestValues(pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO), report),
                    null
            );
            lstfiles.add(fileFormulario);

//            FileDTO fileSolicitud = new FileDTO(
//                    "FT-GS-01 Solicitud de Modificación de Usuario",
//                    pdfUtils.replaceMovementRequestValues(pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD), report),
//                    null
//            );
//            lstfiles.add(fileSolicitud);


            pdfUtils.enviarCorreoConPDF(
                    lstfiles,
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
            FileDTO fileFormulario = new FileDTO(
                    "FT-GT-12 Formulario de Cese",
                    pdfUtils.replaceOutRequestValues(pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.FORMULARIO), report),
                    null
            );

            FileDTO fileSolicitud = new FileDTO(
                    "FT-GS-01 Solicitud de Desactivación de Usuario",
                    pdfUtils.replaceOutRequestValues(pdfUtils.getHtmlTemplate(PDFUtils.TemplateType.SOLICITUD), report),
                    null
            );

            List<FileDTO> lstfiles = new ArrayList<>();
            lstfiles.add(fileFormulario);
            lstfiles.add(fileSolicitud);

            pdfUtils.enviarCorreoConPDF(
                    lstfiles,
                    report.getCorreoGestor(),
                    "Cese empleado",
                    "Formulario de cese del empleado."
            );
        }

        return report.getResponse();
    }
}
