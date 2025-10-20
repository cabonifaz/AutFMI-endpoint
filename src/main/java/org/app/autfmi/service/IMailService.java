package org.app.autfmi.service;

import java.util.List;
import java.util.Map;
import org.app.autfmi.model.dto.RequirementDTO;

import org.app.autfmi.util.MailUtils;

public interface IMailService {

  public MailUtils mailUtils = new MailUtils();

  void sendCreateRequirementNotification(
      String userName,
      RequirementDTO rDto,
      List<Map<String, Object>> vacantesMapped,
      List<Map<String, Object>> contactosMapList,
      List<Map<String, Object>> habilidadesMapped,
      List<Map<String, Object>> carrerasMapped);

  void sendUpdateRequirementNotification(String userName,
      RequirementDTO rDto,
      List<Map<String, Object>> vacantesMapped,
      List<Map<String, Object>> contactosMapList,
      List<Map<String, Object>> habilidadesMapped,
      List<Map<String, Object>> carrerasMapped,
      List<Map<String, Object>> postulanteList);
}
