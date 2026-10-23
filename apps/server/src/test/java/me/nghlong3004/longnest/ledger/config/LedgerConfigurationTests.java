package me.nghlong3004.longnest.ledger.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import me.nghlong3004.longnest.ledger.model.PersonalOwner;
import me.nghlong3004.longnest.ledger.pagination.LedgerCursors;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/** Configuration-only context; never registers DataSource/JPA/Flyway or contacts a database. */
class LedgerConfigurationTests {
  @Test
  void inactiveProfileHasNoLedgerOwnerOrRuntimeBeans() {
    try (var context = configured(false)) {
      context.refresh();
      assertThat(context.getBeansOfType(PersonalOwner.class)).isEmpty();
      assertThat(context.getBeansOfType(LedgerCursors.class)).isEmpty();
    }
  }

  @Test
  void activeProfileRequiresExplicitOwnerAndDoesNotDefaultOpen() {
    try (var context = configured(true)) {
      assertThatThrownBy(context::refresh).hasStackTraceContaining("longnest.ledger.owner");
    }
    try (var context = configured(true)) {
      context.getEnvironment().getPropertySources().addFirst(
          new MapPropertySource("synthetic", Map.of("longnest.ledger.owner", "personal")));
      context.refresh();
      assertThat(context.getBean(PersonalOwner.class).principal()).isEqualTo("personal");
      assertThat(context.getBean(LedgerCursors.class)).isNotNull();
    }
    assertThatThrownBy(() -> new PersonalOwner(" ")).isInstanceOf(IllegalArgumentException.class);
  }

  private AnnotationConfigApplicationContext configured(boolean active) {
    var context = new AnnotationConfigApplicationContext();
    var environment = new StandardEnvironment();
    environment.getPropertySources()
        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    environment.getPropertySources()
        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    environment.setActiveProfiles(active ? "ledger" : "unit");
    context.setEnvironment(environment);
    context.registerBean(PropertySourcesPlaceholderConfigurer.class);
    context.register(LedgerConfiguration.class);
    return context;
  }
}
