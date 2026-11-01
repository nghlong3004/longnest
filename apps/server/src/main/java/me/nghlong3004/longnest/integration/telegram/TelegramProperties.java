package me.nghlong3004.longnest.integration.telegram;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "longnest.telegram")
public record TelegramProperties(@NotBlank String token, @NotNull @Positive Long user) {

  @Override
  public String toString() {
    return "TelegramProperties[token=<redacted>, user=<redacted>]";
  }
}
