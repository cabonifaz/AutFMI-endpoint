package org.app.autfmi.service;

import java.util.List;

import org.app.autfmi.model.dto.UserContactInfoDTO;
import org.app.autfmi.model.report.CeseReport;
import org.app.autfmi.model.report.MovementReport;
import org.app.autfmi.model.report.RequirementReport;
import org.app.autfmi.model.report.SolicitudEquipoReport;
import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.response.InterviewDetailResponseDTO;

public interface IMailService {

	void sendRequirementNotificationV2(RequirementReport report, String subject, List<String> toAddresses,
			List<String> ccAddresses, String action);

	void sendCeseReportNotification(CeseReport report);

	void sendMovementReportNotification(MovementReport report);

	void sendEquipmentRequestNotification(SolicitudEquipoReport report);

	void sendInterviewUnifiedNotification(InterviewDetailResponseDTO detail, String talentEmail, String talentFullName,
			BaseRequest actionUser, UserContactInfoDTO actionUserInfo, String actionType);

}
