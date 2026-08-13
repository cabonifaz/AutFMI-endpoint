package org.app.autfmi.service;

import java.util.List;

import org.app.autfmi.model.dto.UserContactInfoDTO;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.EntryReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.report.RequirementReport;
import org.app.autfmi.model.report.SolicitudEquipoReport;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.InterviewDetailResponseDTO;

public interface IMailService {

	void sendRequirementNotificationV2(RequirementReport report, String subject, List<String> toAddresses,
			List<String> ccAddresses, String action);

	void sendCeseReportNotification(CeseReport report);

	/**
	 * Envía los correos del ingreso: Formulario de Ingreso (maestro 52) y Solicitud
	 * de Creación de Usuario (maestro 51), como dos correos separados, con copia al
	 * usuario generador y al maestro 35.
	 */
	void sendEntryReportNotification(EntryReport report);

	void sendMovementReportNotification(MovementReport report);

	void sendEquipmentRequestNotification(SolicitudEquipoReport report);

	void sendInterviewUnifiedNotification(InterviewDetailResponseDTO detail, String talentEmail, String talentFullName,
			BaseRequest actionUser, UserContactInfoDTO actionUserInfo, String actionType);

	/**
	 * Igual que {@link #sendInterviewUnifiedNotification}, pero adjunta el ICS de la
	 * entrevista. El adjunto es opcional: si {@code icsAttachment} es null/vacío, el
	 * correo se envía igualmente sin adjunto.
	 */
	void sendInterviewUnifiedNotification(InterviewDetailResponseDTO detail, String talentEmail, String talentFullName,
			BaseRequest actionUser, UserContactInfoDTO actionUserInfo, String actionType,
			byte[] icsAttachment, String icsFileName);

}
