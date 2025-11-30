package org.folio.app.generator.model.registry.artifact;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class DockerHubArtifactRegistryTest {

  @Test
  void isValid_positive() {
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    assertThat(registry.isValid()).isTrue();
  }

  @Test
  void isValid_negative_blankNamespace() {
    var registry = new DockerHubArtifactRegistry().namespace("");

    assertThat(registry.isValid()).isFalse();
  }

  @Test
  void isValid_negative_nullNamespace() {
    var registry = new DockerHubArtifactRegistry();

    assertThat(registry.isValid()).isFalse();
  }

  @Test
  void isValid_negative_blankBaseUrl() {
    var registry = new DockerHubArtifactRegistry().namespace("folioorg").baseUrl("");

    assertThat(registry.isValid()).isFalse();
  }

  @Test
  void isValid_negative_malformedBaseUrl() {
    var registry = new DockerHubArtifactRegistry().namespace("folioorg").baseUrl("not-a-valid-url");

    assertThat(registry.isValid()).isFalse();
  }

  @Test
  void getType_positive() {
    var registry = new DockerHubArtifactRegistry();

    assertThat(registry.getType()).isEqualTo(ArtifactRegistryType.DOCKER_HUB);
  }

  @Test
  void defaultBaseUrl_positive() {
    var registry = new DockerHubArtifactRegistry();

    assertThat(registry.getBaseUrl()).isEqualTo(DockerHubArtifactRegistry.DEFAULT_BASE_URL);
  }
}
