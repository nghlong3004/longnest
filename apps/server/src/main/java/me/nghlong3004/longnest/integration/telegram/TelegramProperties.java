package me.nghlong3004.longnest.integration.telegram;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "longnest.telegram")
public record TelegramProperties(
    @NotBlank String token, @NotEmpty Set<@NotNull @Positive Long> users) {

  public TelegramProperties {
    if (users != null) {
      users = Set.copyOf(users);
    }
  }

  @Override
  public String toString() {
    return "TelegramProperties[token=<redacted>, users=<redacted>]";
  }
}
