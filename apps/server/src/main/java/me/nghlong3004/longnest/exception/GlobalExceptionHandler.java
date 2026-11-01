package me.nghlong3004.longnest.exception;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/** Resolves safe error messages at application boundaries, including Telegram polling. */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final MessageSource messages;

  /**
   * Logs safe diagnostic metadata and selects a localized message for an exception.
   *
   * @param exception the failure caught at the application boundary
   * @param locale the language to use for the user-facing message
   * @return a localized message that does not expose exception messages or causes
   */
  public String handle(Exception exception, Locale locale) {
    ErrorCode code;
    if (exception instanceof ResourceException resourceException) {
      code = resourceException.getErrorCode();
      var cause = resourceException.getCause();
      log.warn(
          "Application error code={} causeType={}",
          code,
          cause == null ? "none" : cause.getClass().getName());
    } else {
      code = ErrorCode.INTERNAL_ERROR;
      var stack = exception.getStackTrace();
      log.error(
          "Unexpected application error code={} type={} at={}",
          code,
          exception.getClass().getName(),
          stack.length == 0 ? "unknown" : stack[0]);
    }
    return messages.getMessage(code.getMessageKey(), null, locale);
  }
}
