package me.nghlong3004.longnest;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.task.AsyncTaskExecutor;

public class TestLongnestApplication {

  public static void main(String[] args) throws Exception {
    try (var context = application().run("--spring.profiles.active=smoke")) {
      var executor = context.getBean("applicationTaskExecutor", AsyncTaskExecutor.class);
      if (!executor.submit(() -> Thread.currentThread().isVirtual()).get(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Managed executor did not use a virtual thread");
      }
      System.out.println("Smoke passed: non-web context and managed virtual-thread execution");
    }
  }

  static SpringApplication application() {
    // Keep synthetic checks independent of private host credentials and external config files.
    var environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    environment
        .getPropertySources()
        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    var application = new SpringApplication(LongnestApplication.class);
    application.setEnvironment(environment);
    application.setDefaultProperties(
        Map.of("spring.config.location", "classpath:/application.yaml"));
    return application;
  }
}
