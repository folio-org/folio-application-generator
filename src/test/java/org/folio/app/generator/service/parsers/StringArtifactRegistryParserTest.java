package org.folio.app.generator.service.parsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.folio.app.generator.model.registry.artifact.DockerHubArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.FolioNpmArtifactRegistry;
import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class StringArtifactRegistryParserTest {

  private StringArtifactRegistryParser parser;

  @BeforeEach
  void setUp() {
    this.parser = new StringArtifactRegistryParser();
  }

  @Test
  void parseDocker_positive_namespaceOnly() {
    var result = parser.parseDocker("folioorg");

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(DockerHubArtifactRegistry.class);
    assertThat(result.get().getNamespace()).isEqualTo("folioorg");
    assertThat(result.get().getBaseUrl()).isEqualTo("https://hub.docker.com/v2/repositories");
  }

  @Test
  void parseDocker_positive_urlAndNamespace() {
    var result = parser.parseDocker("https://custom-registry.io::custom-namespace");

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(DockerHubArtifactRegistry.class);
    assertThat(result.get().getNamespace()).isEqualTo("custom-namespace");
    assertThat(result.get().getBaseUrl()).isEqualTo("https://custom-registry.io");
  }

  @Test
  void parseDocker_positive_urlWithTrailingSlash() {
    var result = parser.parseDocker("https://custom-registry.io/::namespace");

    assertThat(result).isPresent();
    assertThat(result.get().getBaseUrl()).isEqualTo("https://custom-registry.io");
  }

  @Test
  void parseDocker_positive_blankString() {
    var result = parser.parseDocker("  ");

    assertThat(result).isEmpty();
  }

  @Test
  void parseDocker_positive_nullString() {
    var result = parser.parseDocker(null);

    assertThat(result).isEmpty();
  }

  @Test
  void parseDocker_negative_invalidUrl() {
    assertThatThrownBy(() -> parser.parseDocker("invalid-url::namespace"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid url provided");
  }

  @Test
  void parseNpm_positive_namespaceOnly() {
    var result = parser.parseNpm("npm-folio");

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(FolioNpmArtifactRegistry.class);
    assertThat(result.get().getNamespace()).isEqualTo("npm-folio");
    assertThat(result.get().getBaseUrl()).isEqualTo("https://repository.folio.org/repository");
  }

  @Test
  void parseNpm_positive_urlAndNamespace() {
    var result = parser.parseNpm("https://custom-npm.io::custom-repo");

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(FolioNpmArtifactRegistry.class);
    assertThat(result.get().getNamespace()).isEqualTo("custom-repo");
    assertThat(result.get().getBaseUrl()).isEqualTo("https://custom-npm.io");
  }

  @Test
  void parseNpm_positive_blankString() {
    var result = parser.parseNpm("  ");

    assertThat(result).isEmpty();
  }

  @Test
  void parseNpm_positive_nullString() {
    var result = parser.parseNpm(null);

    assertThat(result).isEmpty();
  }

  @Test
  void parse_positive_dockerType() {
    var result = parser.parse("test-namespace", ArtifactRegistryType.DOCKER_HUB);

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(DockerHubArtifactRegistry.class);
    assertThat(result.get().getNamespace()).isEqualTo("test-namespace");
  }

  @Test
  void parse_positive_npmType() {
    var result = parser.parse("test-repo", ArtifactRegistryType.FOLIO_NPM);

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(FolioNpmArtifactRegistry.class);
    assertThat(result.get().getNamespace()).isEqualTo("test-repo");
  }

  @Test
  void parseDocker_positive_trimmedValue() {
    var result = parser.parseDocker("  folioorg  ");

    assertThat(result).isPresent();
    assertThat(result.get().getNamespace()).isEqualTo("folioorg");
  }

  @Test
  void parseNpm_positive_trimmedValue() {
    var result = parser.parseNpm("  npm-folio  ");

    assertThat(result).isPresent();
    assertThat(result.get().getNamespace()).isEqualTo("npm-folio");
  }
}
