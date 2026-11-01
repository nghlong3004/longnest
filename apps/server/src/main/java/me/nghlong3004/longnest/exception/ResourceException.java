package me.nghlong3004.longnest.exception;

import java.io.Serial;
import java.util.Objects;
import lombok.Getter;
import me.nghlong3004.longnest.model.enums.ErrorCode;

/** Represents an expected application failure using a stable error code. */
@Getter
public class ResourceException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final ErrorCode errorCode;

  public ResourceException(ErrorCode errorCode) {
    this(errorCode, null);
  }

  public ResourceException(ErrorCode errorCode, Throwable cause) {
    super(Objects.requireNonNull(errorCode, "Error code is required").name(), cause);
    this.errorCode = errorCode;
  }
}
