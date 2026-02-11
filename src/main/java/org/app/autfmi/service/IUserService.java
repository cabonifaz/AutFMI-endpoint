package org.app.autfmi.service;

import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.response.OperationResult;
import org.springframework.web.multipart.MultipartFile;

public interface IUserService {

  public OperationResult<UserDTO> uploadSignature(Integer userId, MultipartFile file, String authToken);

}
