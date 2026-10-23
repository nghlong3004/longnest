package me.nghlong3004.longnest.presentation;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/** Fixed Vietnamese presentation, reusable by server adapters without Telegram dependencies. */
@Component
@RequiredArgsConstructor
public class UiMessages {
  private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
  private final MessageSource source;

  public String text(String key, Object... arguments) {
    String fallback = source.getMessage("ui.unavailable", null, VIETNAMESE);
    return source.getMessage(key, arguments, fallback, VIETNAMESE);
  }
}
