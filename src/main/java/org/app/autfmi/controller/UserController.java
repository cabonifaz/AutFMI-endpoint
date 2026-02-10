package org.app.autfmi.controller;

import org.app.autfmi.model.response.BaseResponse;
import org.app.autfmi.service.IUserService;
import org.app.autfmi.util.JwtHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/users")
public class UserController {

  private final IUserService userService;

  public UserController(IUserService userService) {
    this.userService = userService;
  }

  @PostMapping(value = "/upload-signature", consumes = { "multipart/form-data" })
  public ResponseEntity<BaseResponse> uploadSignature(
      @RequestParam("userId") Integer userId,
      @RequestParam("file") MultipartFile file,
      HttpServletRequest httpServletRequest) {

    try {
      var token = JwtHelper.extractToken(httpServletRequest);
      var updatedUser = userService.uploadSignature(userId, file, token);
      return ResponseEntity.ok(updatedUser.getBaseResponse());
    } catch (Exception e) {
      return ResponseEntity.ok(new BaseResponse(3, e.getMessage()));
    }

  }

}
