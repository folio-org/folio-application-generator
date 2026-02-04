package org.folio.app.generator.conditions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.app.generator.model.types.ArtifactRegistryType.DOCKER_HUB;
import static org.folio.app.generator.model.types.ArtifactRegistryType.FOLIO_NPM;
import static org.folio.app.generator.model.types.RegistryType.AWS_S3;
import static org.folio.app.generator.model.types.RegistryType.OKAPI;

import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@UnitTest
class ArtifactValidationConditionTest {

  private AnnotationConfigApplicationContext context;

  @BeforeEach
  void setUp() {
    context = new AnnotationConfigApplicationContext();
    // Clear any lingering properties from previous tests
    var properties = context.getEnvironment().getSystemProperties();
    properties.remove(DOCKER_HUB.getPropertyName());
    properties.remove(FOLIO_NPM.getPropertyName());
    properties.remove(AWS_S3.getPropertyName());
    properties.remove(OKAPI.getPropertyName());
  }

  @AfterEach
  void tearDown() {
    var properties = context.getEnvironment().getSystemProperties();
    properties.remove(DOCKER_HUB.getPropertyName());
    properties.remove(FOLIO_NPM.getPropertyName());
    properties.remove(AWS_S3.getPropertyName());
    properties.remove(OKAPI.getPropertyName());
    context.close();
  }

  @Test
  void matches_positive_whenDockerHubEnabled() {
    context.getEnvironment().getSystemProperties().put(DOCKER_HUB.getPropertyName(), true);
    context.register(TestConfiguration.class);
    context.refresh();

    assertThat(context.containsBean("testBean")).isTrue();
  }

  @Test
  void matches_positive_whenFolioNpmEnabled() {
    context.getEnvironment().getSystemProperties().put(FOLIO_NPM.getPropertyName(), true);
    context.register(TestConfiguration.class);
    context.refresh();

    assertThat(context.containsBean("testBean")).isTrue();
  }

  @Test
  void matches_positive_whenBothDockerHubAndFolioNpmEnabled() {
    context.getEnvironment().getSystemProperties().put(DOCKER_HUB.getPropertyName(), true);
    context.getEnvironment().getSystemProperties().put(FOLIO_NPM.getPropertyName(), true);
    context.register(TestConfiguration.class);
    context.refresh();

    assertThat(context.containsBean("testBean")).isTrue();
  }

  @Test
  void matches_negative_whenNoArtifactValidationEnabled() {
    context.register(TestConfiguration.class);
    context.refresh();

    assertThat(context.containsBean("testBean")).isFalse();
  }

  @Test
  void matches_negative_whenOnlyS3Enabled() {
    // S3 is a module registry, not an artifact validation registry
    context.getEnvironment().getSystemProperties().put(AWS_S3.getPropertyName(), true);
    context.register(TestConfiguration.class);
    context.refresh();

    assertThat(context.containsBean("testBean")).isFalse();
  }

  @Test
  void matches_negative_whenOnlyOkapiEnabled() {
    // OKAPI is a module registry, not an artifact validation registry
    context.getEnvironment().getSystemProperties().put(OKAPI.getPropertyName(), true);
    context.register(TestConfiguration.class);
    context.refresh();

    assertThat(context.containsBean("testBean")).isFalse();
  }

  @Configuration
  static class TestConfiguration {

    @Bean
    @Conditional(ArtifactValidationCondition.class)
    public String testBean() {
      return "test";
    }
  }
}
