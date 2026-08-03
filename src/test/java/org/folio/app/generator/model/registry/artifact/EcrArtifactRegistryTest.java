package org.folio.app.generator.model.registry.artifact;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class EcrArtifactRegistryTest {

  private static final String VALID_ECR_URL = "https://123456789012.dkr.ecr.us-west-2.amazonaws.com";

  @Test
  void isValid_positive_withNamespace() {
    var registry = new EcrArtifactRegistry().baseUrl(VALID_ECR_URL).namespace("folio");

    assertThat(registry.isValid()).isTrue();
  }

  @Test
  void isValid_positive_withoutNamespace() {
    var registry = new EcrArtifactRegistry().baseUrl(VALID_ECR_URL);

    assertThat(registry.isValid()).isTrue();
  }

  @Test
  void isValid_negative_nullBaseUrl() {
    var registry = new EcrArtifactRegistry();

    assertThat(registry.isValid()).isFalse();
  }

  @Test
  void isValid_negative_blankBaseUrl() {
    var registry = new EcrArtifactRegistry().baseUrl("");

    assertThat(registry.isValid()).isFalse();
  }

  @Test
  void isValid_negative_malformedBaseUrl() {
    var registry = new EcrArtifactRegistry().baseUrl("not-a-valid-url");

    assertThat(registry.isValid()).isFalse();
  }

  @Test
  void getType_positive() {
    var registry = new EcrArtifactRegistry();

    assertThat(registry.getType()).isEqualTo(ArtifactRegistryType.AWS_ECR);
  }

  @Test
  void defaultBaseUrl_positive_isNull() {
    var registry = new EcrArtifactRegistry();

    assertThat(registry.getBaseUrl()).isNull();
  }
}
