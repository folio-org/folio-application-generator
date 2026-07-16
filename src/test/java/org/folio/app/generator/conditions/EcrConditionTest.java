package org.folio.app.generator.conditions;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@UnitTest
class EcrConditionTest {

  private static final String ECR_PROPERTY = "folio-app-generator.aws-ecr.enabled";

  private AnnotationConfigApplicationContext context;

  @BeforeEach
  void setUp() {
    System.getProperties().remove(ECR_PROPERTY);
  }

  @AfterEach
  void tearDown() {
    System.getProperties().remove(ECR_PROPERTY);
    if (context != null) {
      context.close();
    }
  }

  @Test
  void matches_positive_whenEcrEnabled() {
    context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getSystemProperties()
      .put("folio-app-generator.aws-ecr.enabled", true);
    context.register(TestComponent.class);
    context.refresh();

    assertThat(context.containsBean("testComponent")).isTrue();
  }

  @Test
  void matches_negative_whenEcrDisabled() {
    context = new AnnotationConfigApplicationContext();
    context.register(TestComponent.class);
    context.refresh();

    assertThat(context.containsBean("testComponent")).isFalse();
  }

  @Test
  void matches_negative_whenEcrExplicitlyDisabled() {
    context = new AnnotationConfigApplicationContext();
    context.getEnvironment().getSystemProperties()
      .put("folio-app-generator.aws-ecr.enabled", false);
    context.register(TestComponent.class);
    context.refresh();

    assertThat(context.containsBean("testComponent")).isFalse();
  }

  @Component("testComponent")
  @Conditional(EcrCondition.class)
  static class TestComponent {
  }
}
