package me.nghlong3004.longnest.integration.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.telegram.telegrambots.meta.generics.TelegramClient;

class TelegramConfigurationTests {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(TelegramConfiguration.class);

  @Test
  void staysDisabledWithoutCredentials() {
    runner.run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context).doesNotHaveBean(TelegramClient.class);
      assertThat(context).doesNotHaveBean(TelegramProperties.class);
    });
  }

  @Test
  void validatesCredentialsWhenEnabled() {
    runner
        .withPropertyValues("longnest.telegram.enabled=true", "longnest.telegram.users=0")
        .run(context -> assertThat(context).hasFailed());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "0", "-1", "123,0", "abc"})
  void rejectsEmptyOrInvalidUserAllowlist(String users) {
    runner
        .withPropertyValues(
            "longnest.telegram.enabled=true",
            "longnest.telegram.token=synthetic-token",
            "longnest.telegram.users=" + users)
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void bindsCredentialsAndCreatesManagedClientsWithoutRegisteringABot() {
    runner
        .withPropertyValues(
            "longnest.telegram.enabled=true",
            "longnest.telegram.token=synthetic-token",
            "longnest.telegram.users=123,789")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(TelegramClient.class);
          var properties = context.getBean(TelegramProperties.class);
          assertThat(properties.users()).containsExactlyInAnyOrder(123L, 789L);
          assertThat(properties.toString()).doesNotContain(properties.token());
        });
  }
}
