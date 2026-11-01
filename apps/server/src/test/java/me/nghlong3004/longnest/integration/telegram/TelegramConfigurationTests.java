package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.generics.TelegramClient;

class TelegramConfigurationTests {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(TelegramConfiguration.class);

  @Test
  void staysDisabledWithoutCredentials() {
    runner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).doesNotHaveBean(TelegramClient.class);
          assertThat(context).doesNotHaveBean(TelegramProperties.class);
        });
  }

  @Test
  void validatesCredentialsWhenEnabled() {
    runner
        .withPropertyValues("longnest.telegram.enabled=true", "longnest.telegram.user=0")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void bindsCredentialsAndCreatesManagedClientsWithoutRegisteringABot() {
    runner
        .withPropertyValues(
            "longnest.telegram.enabled=true",
            "longnest.telegram.token=synthetic-token",
            "longnest.telegram.user=123")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(TelegramClient.class);
              var properties = context.getBean(TelegramProperties.class);
              assertThat(properties.user()).isEqualTo(123L);
              assertThat(properties.toString()).doesNotContain(properties.token());
            });
  }
}
