package org.app.autfmi.service.impl;

import org.app.autfmi.model.request.BaseRequest;
import org.app.autfmi.model.request.InterviewRequest;
import org.app.autfmi.model.response.OperationResult;
import org.app.autfmi.repository.InterviewRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterviewService {

  private final InterviewRepository interviewRepository;

  /**
   * Create Interview
   * 
   * @param request
   * @param baseRequest
   * @return
   */
  public OperationResult<Integer> createInterview(
      InterviewRequest request,
      BaseRequest baseRequest) {
    return this.interviewRepository.createInterview(request, baseRequest);
  }
}
