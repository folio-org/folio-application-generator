package org.folio.app.generator.conditions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.app.generator.model.types.ArtifactRegistryType.DOCKER_HUB;
import static org.folio.app.generator.model.types.ArtifactRegistryType.FOLIO_NPM;
import static org.folio.app.generator.model.types.RegistryType.AWS_S3;
import static org.folio.app.generator.model.types.RegistryType.OKAPI;
import static org.folio.app.generator.model.types.RegistryType.SIMPLE;

import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@UnitTest
class HttpConditionTest {

  private AnnotationConfigApplicationContext context;

  @BeforeEach
  void setUp() {
    context = new AnnotationConfigApplicationContext();
    // Clear any lingering properties from previous tests
    var properties = context.getEnvironment().getSystemProperties();
    properties.remove(OKAPI.getPropertyName());
    properties.remove(SIMPLE.getPropertyName());
    properties.remove(AWS_S3.getPropertyName());
    properties.remove(DOCKER_HUB.getPropertyName());
    properties.remove(FOLIO_NPM.getPropertyName());
  }

  @AfterEach
  void tearDown() {
    var properties = context.getEnvironment().getSystemProperties();
    properties.remove(OKAPI.getPropertyName());
    properties.remove(SIMPLE.getPropertyName());
    properties.remove(AWS_S3.getPropertyName());
    properties.remove(DOCKER_HUB.getPropertyName());
    properties.remove(FOLIO_NPM.getPropertyName());
    context.close();
  }

  @Test
  void matches_positive_whenOkapiEnabled() {
    context.getEnvironment().getSystemProperties().put(OKAPI.getPropertyName(), true);
    context.register(TestConfiguration.class);
    context.refresh();

    assertThat(context.containsBean("testBean")).isTrue();
  }

  @Test
  void matches_positive_whenSimpleEnabled() {
    context.getEnvironment().getSystemProperties().put(SIMPLE.getPropertyName(), true);
    context.register(TestConfiguration.class);
    context.refresh();

    assertThat(context.containsBean("testBean")).isTrue();
  }

  @Test
  void matches_positive_whenBothOkapiAndSimpleEnabled() {
    context.getEnvironment().getSystemProperties().put(OKAPI.getPropertyName(), true);
    context.getEnvironment().getSystemProperties().put(SIMPLE.getPropertyName(), true);
    context.register(TestConfiguration.class);
    context.refresh();

    assertThat(context.containsBean("testBean")).isTrue();
  }

  @Test
  void matches_negative_whenOnlyS3Enabled() {
    context.getEnvironment().getSystemProperties().put(AWS_S3.getPropertyName(), true);
    context.register(TestConfiguration.class);
    context.refresh();

    assertThat(context.containsBean("testBean")).isFalse();
  }

  @Test
  void matches_negative_whenNoRegistriesEnabled() {
    context.register(TestConfiguration.class);
    context.refresh();

    assertThat(context.containsBean("testBean")).isFalse();
  }

  @Test
  void matches_positive_whenDockerHubEnabled() {
    // After adding artifact validation to HttpCondition, this should PASS
    context.getEnvironment().getSystemProperties().put(DOCKER_HUB.getPropertyName(), true);
    context.register(TestConfiguration.class);
    context.refresh();

    // Should be TRUE after fix - HttpCondition now includes artifact validation
    assertThat(context.containsBean("testBean")).isTrue();
  }

  @Test
  void matches_positive_whenFolioNpmEnabled() {
    // After adding artifact validation to HttpCondition, this should PASS
    context.getEnvironment().getSystemProperties().put(FOLIO_NPM.getPropertyName(), true);
    context.register(TestConfiguration.class);
    context.refresh();

    // Should be TRUE after fix - HttpCondition now includes artifact validation
    assertThat(context.containsBean("testBean")).isTrue();
  }

  @Test
  void matches_positive_notNull() {
    assertThat(new HttpCondition.Okapi()).isNotNull();
    assertThat(new HttpCondition.Simple()).isNotNull();
    assertThat(new HttpCondition.ArtifactValidation()).isNotNull();
  }

  @Configuration
  static class TestConfiguration {

    @Bean
    @Conditional(HttpCondition.class)
    public String testBean() {
      return "test";
    }
  }
}
