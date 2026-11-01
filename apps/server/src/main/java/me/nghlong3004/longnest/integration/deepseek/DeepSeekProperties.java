package me.nghlong3004.longnest.integration.deepseek;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "longnest.deepseek")
public record DeepSeekProperties(
    @NotBlank String apiKey, @NotBlank String baseUrl, @NotBlank String model) {

  @Override
  public String toString() {
    return "DeepSeekProperties[apiKey=<redacted>, baseUrl=<redacted>, model=" + model + "]";
  }
}
