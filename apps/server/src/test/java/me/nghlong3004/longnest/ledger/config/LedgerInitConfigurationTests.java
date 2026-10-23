package me.nghlong3004.longnest.ledger.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import me.nghlong3004.longnest.ledger.model.HostContext;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.service.TransactionService;
import me.nghlong3004.longnest.telegram.config.TelegramConfiguration;
import me.nghlong3004.longnest.telegram.config.TelegramLifecycle;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Profiles;

/** Synthetic profile/runner checks; no DataSource, Flyway, Telegram or socket is created. */
class LedgerInitConfigurationTests {
  @Test
  void initProfileExpandsLedgerAndRunnerClosesSuccessfulApplication() {
    var application = new SpringApplication(SyntheticInitApplication.class);
    application.setWebApplicationType(WebApplicationType.NONE);
    application.setDefaultProperties(Map.of("longnest.telegram.enabled", "true"));
    try (var context = application.run("--spring.profiles.active=ledger-init")) {
      assertThat(context.getEnvironment().acceptsProfiles(Profiles.of("ledger"))).isTrue();
      assertThat(context.isActive()).isFalse();
      verify(SyntheticInitApplication.TRANSACTIONS).initializeOwner(
          new HostContext("personal", null, null, null));
    }
  }

  @Test
  void inheritedTelegramEnableCannotActivateInitProfile() {
    new ApplicationContextRunner().withUserConfiguration(TelegramConfiguration.class)
        .withInitializer(context -> context.getEnvironment()
            .setActiveProfiles("ledger-init", "ledger"))
        .withPropertyValues("longnest.telegram.enabled=true")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).doesNotHaveBean(TelegramLifecycle.class);
        });
  }

  @Test
  void ordinaryLedgerProfileHasNoInitializer() {
    new ApplicationContextRunner().withUserConfiguration(LedgerInitConfiguration.class)
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("ledger"))
        .run(context -> assertThat(context).doesNotHaveBean(ApplicationRunner.class));
  }

  @Test
  void failureClosesContextAndHidesPersistenceDetails() {
    var service = mock(TransactionService.class);
    var context = mock(ConfigurableApplicationContext.class);
    doThrow(new IllegalStateException("jdbc:secret-owner-token"))
        .when(service).initializeOwner(any());
    var runner = new LedgerInitConfiguration().initializeLedgerOwner(
        new PersonalOwner("personal"), service, context);
    assertThatThrownBy(() -> runner.run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Ledger initialization failed; inspect storage before retry")
        .hasNoCause();
    verify(context).close();
  }

  @SpringBootConfiguration
  @Import({LedgerInitConfiguration.class, TelegramConfiguration.class})
  static class SyntheticInitApplication {
    private static final TransactionService TRANSACTIONS = mock(TransactionService.class);

    @Bean
    PersonalOwner personalOwner() {
      return new PersonalOwner("personal");
    }

    @Bean
    TransactionService transactionService() {
      return TRANSACTIONS;
    }
  }
}
