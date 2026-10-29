package me.nghlong3004.longnest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LongnestApplicationTests {

  @Test
  void normalRuntimeFailsClosedWithoutDatabaseConfiguration() {
    assertThatThrownBy(
            () -> TestLongnestApplication.application().run("--spring.main.keep-alive=false"))
        .hasStackTraceContaining("Failed to determine a suitable driver class");
  }
}
