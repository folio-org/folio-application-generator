package org.folio.app.generator.model.registry.artifact;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class FolioNpmArtifactRegistryTest {

  @Test
  void isValid_positive() {
    var registry = new FolioNpmArtifactRegistry().namespace("npm-folio");

    assertThat(registry.isValid()).isTrue();
  }

  @Test
  void isValid_negative_blankNamespace() {
    var registry = new FolioNpmArtifactRegistry().namespace("");

    assertThat(registry.isValid()).isFalse();
  }

  @Test
  void isValid_negative_nullNamespace() {
    var registry = new FolioNpmArtifactRegistry();

    assertThat(registry.isValid()).isFalse();
  }

  @Test
  void isValid_negative_blankBaseUrl() {
    var registry = new FolioNpmArtifactRegistry().namespace("npm-folio").baseUrl("");

    assertThat(registry.isValid()).isFalse();
  }

  @Test
  void isValid_negative_malformedBaseUrl() {
    var registry = new FolioNpmArtifactRegistry().namespace("npm-folio").baseUrl("not-a-valid-url");

    assertThat(registry.isValid()).isFalse();
  }

  @Test
  void getType_positive() {
    var registry = new FolioNpmArtifactRegistry();

    assertThat(registry.getType()).isEqualTo(ArtifactRegistryType.FOLIO_NPM);
  }

  @Test
  void defaultBaseUrl_positive() {
    var registry = new FolioNpmArtifactRegistry();

    assertThat(registry.getBaseUrl()).isEqualTo(FolioNpmArtifactRegistry.DEFAULT_BASE_URL);
  }
}
