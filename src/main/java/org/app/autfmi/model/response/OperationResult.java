package org.app.autfmi.model.response;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperationResult<T> {
  @NonNull
  private BaseResponse baseResponse;

  @Nullable
  private T data;
}
