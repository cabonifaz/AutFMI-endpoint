package org.app.autfmi.service;

import java.util.List;
import org.app.autfmi.model.report.RequirementReport;

public interface IMailService {

	void sendRequirementNotificationV2(RequirementReport report, String subject, List<String> toAddresses,
			List<String> ccAddresses, String action);
}
