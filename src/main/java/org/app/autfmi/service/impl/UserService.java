package org.app.autfmi.service.impl;

import java.io.IOException;

import org.app.autfmi.model.dto.UserDTO;
import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.repository.UserRepository;
import org.app.autfmi.service.IUserService;
import org.app.autfmi.util.Common;
import org.app.autfmi.util.Constante;
import org.app.autfmi.util.FileUtils;
import org.app.autfmi.util.JwtHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService implements IUserService {

  private final Logger logger = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;
  private final JwtHelper jwtHelper;

  public UserService(UserRepository userRepository, JwtHelper jwtHelper) {
    this.userRepository = userRepository;
    this.jwtHelper = jwtHelper;
  }

  @Override
  public OperationResult<UserDTO> uploadSignature(Integer userId, MultipartFile file, String authToken) {

    var user = jwtHelper.decodeToken(authToken);
    var baseRequest = Common.createBaseRequest(user, "");

    this.logger.info("Registrando firma de usuario: {}", user.getIdUsuario());
    this.logger.info("Para: {}", userId);

    // Subir la firma al S3
    var path = new StringBuilder()
        .append(Constante.RUTA_REPOSITORIO)
        .append(user.getIdEmpresa())
        .append(Constante.RUTA_FIRMAS.replace("[ID_USUARIO]", userId.toString()))
        .append(file.getOriginalFilename())
        .toString();

    this.logger.info("Path: {}", path);

    byte[] fileBytes = null;
    try {
      fileBytes = file.getBytes();
    } catch (IOException e) {
      var operationResult = new OperationResult<UserDTO>();
      operationResult.setBaseResponse(new BaseResponse(3, e.getMessage()));
      return operationResult;
    }

    var fileBase64 = FileUtils.bytesToBase64(fileBytes);

    var isSaved = FileUtils.guardarArchivoAws(fileBase64, path);

    if (!isSaved) {
      var operationResult = new OperationResult<UserDTO>();
      operationResult.setBaseResponse(new BaseResponse(3, "Error al subir la firma a AWS"));
      this.logger.error("Error al subir la firma a AWS");
      return operationResult;
    }

    // Actualizar la ruta en la base de datos y retornar
    return this.userRepository.uploadSignature(userId, path, baseRequest);
  }

}
