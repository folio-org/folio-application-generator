package org.folio.app.generator.conditions;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@UnitTest
class DockerHubConditionTest {

  private AnnotationConfigApplicationContext context;

  @AfterEach
  void tearDown() {
    if (context != null) {
      context.close();
    }
  }

  @Test
  void matches_positive_whenDockerHubEnabled() {
    context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getSystemProperties()
      .put("folio-app-generator.docker-hub.enabled", true);
    context.register(TestComponent.class);
    context.refresh();

    assertThat(context.containsBean("testComponent")).isTrue();
  }

  @Test
  void matches_negative_whenDockerHubDisabled() {
    context = new AnnotationConfigApplicationContext();
    // Property not set - DockerHub is disabled
    context.register(TestComponent.class);
    context.refresh();

    assertThat(context.containsBean("testComponent")).isFalse();
  }

  @Test
  void matches_negative_whenDockerHubExplicitlyDisabled() {
    context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getSystemProperties()
      .put("folio-app-generator.docker-hub.enabled", false);
    context.register(TestComponent.class);
    context.refresh();

    assertThat(context.containsBean("testComponent")).isFalse();
  }

  @Component("testComponent")
  @Conditional(DockerHubCondition.class)
  static class TestComponent {
  }
}
