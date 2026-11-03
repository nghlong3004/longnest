package me.nghlong3004.longnest.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import me.nghlong3004.longnest.model.enums.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class GlobalExceptionHandlerTests {

  private final ResourceBundleMessageSource messages = messageSource();
  private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messages);

  @Test
  void selectsDifferentVietnameseMessagesByErrorCodeWithoutExposingCauses() {
    var failure = new IllegalStateException("private API key and user message");
    var unavailable =
        handler.handle(
            new ResourceException(ErrorCode.CHATBOT_UNAVAILABLE, failure),
            Locale.forLanguageTag("vi"));
    var empty =
        handler.handle(
            new ResourceException(ErrorCode.CHATBOT_EMPTY_RESPONSE), Locale.forLanguageTag("vi"));

    assertThat(unavailable).contains("kết nối").doesNotContain(failure.getMessage());
    assertThat(empty).contains("chưa có câu trả lời").isNotEqualTo(unavailable);
  }

  @Test
  void mapsUnexpectedExceptionsToSafeInternalError() {
    var exception = new NullPointerException("sensitive implementation detail");
    var result = handler.handle(exception, Locale.forLanguageTag("vi"));
    assertThat(result)
        .isEqualTo(
            messages.getMessage(
                ErrorCode.INTERNAL_ERROR.getMessageKey(), null, Locale.forLanguageTag("vi")))
        .doesNotContain(exception.getMessage());
  }

  @Test
  void resolvesEveryErrorCodeInRootAndVietnameseBundles() {
    for (var code : ErrorCode.values()) {
      for (var locale : new Locale[] {Locale.ROOT, Locale.forLanguageTag("vi")}) {
        var result = handler.handle(new ResourceException(code), locale);
        assertThat(result).isNotBlank().isNotEqualTo(code.getMessageKey());
      }
    }
  }

  private static ResourceBundleMessageSource messageSource() {
    var messages = new ResourceBundleMessageSource();
    messages.setBasename("messages");
    messages.setDefaultEncoding("UTF-8");
    messages.setFallbackToSystemLocale(false);
    return messages;
  }
}
