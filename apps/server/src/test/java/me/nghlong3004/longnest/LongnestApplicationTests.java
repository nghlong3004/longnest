package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManagerFactory;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import me.nghlong3004.longnest.telegram.config.TelegramLifecycle;
import me.nghlong3004.longnest.telegram.model.TelegramSettings;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;

class LongnestApplicationTests {

  @Test
  void smokeStartsWithoutExternalIntegrationsUsesVirtualThreadsAndCloses() throws Exception {
    ConfigurableApplicationContext context;
    try (var started =
        TestLongnestApplication.application().run("--spring.profiles.active=smoke")) {
      context = started;
      assertThat(context.isActive()).isTrue();
      assertThat(context).isNotInstanceOf(WebServerApplicationContext.class);
      assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
      assertThat(context.getBeansOfType(EntityManagerFactory.class)).isEmpty();
      assertThat(context.getBeansOfType(Flyway.class)).isEmpty();
      assertThat(context.getBeansOfType(ChatModel.class)).isEmpty();
      assertThat(context.getBeansOfType(JavaMailSender.class)).isEmpty();
      assertThat(context.getBeansOfType(TelegramLifecycle.class)).isEmpty();
      assertThat(context.getBeansOfType(TelegramSettings.class)).isEmpty();
      assertThat(context.getBeansOfType(com.fasterxml.jackson.databind.ObjectMapper.class)).isEmpty();
      var executor = context.getBean("applicationTaskExecutor", AsyncTaskExecutor.class);
      assertThat(executor.submit(() -> Thread.currentThread().isVirtual()).get(5, TimeUnit.SECONDS))
          .isTrue();
      assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"))
          .isEqualTo("validate");
      assertThat(context.getEnvironment().getProperty("spring.sql.init.mode")).isEqualTo("never");
    }
    assertThat(context.isActive()).isFalse();
  }

  @Test
  void normalRuntimeFailsClosedWithoutDatabaseConfiguration() {
    assertThatThrownBy(
            () -> TestLongnestApplication.application().run("--spring.main.keep-alive=false"))
        .hasStackTraceContaining("Failed to determine a suitable driver class");
  }
}
