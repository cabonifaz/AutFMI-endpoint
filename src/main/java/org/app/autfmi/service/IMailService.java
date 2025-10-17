package org.app.autfmi.service;

import java.util.List;
import java.util.Map;

import org.app.autfmi.util.MailUtils;

public interface IMailService {

  public MailUtils mailUtils = new MailUtils();

  void sendCreateRequirementNotification(
      String userName,
      org.app.autfmi.model.dto.RequirementDTO rDto,
      List<Map<String, Object>> vacantesMapped,
      List<Map<String, Object>> contactosMapList,
      List<Map<String, Object>> habilidadesMapped,
      List<Map<String, Object>> carrerasMapped);
}
