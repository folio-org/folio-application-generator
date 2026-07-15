package org.folio.app.generator.service.parsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.folio.app.generator.model.registry.artifact.DockerHubArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.EcrArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.FolioNpmArtifactRegistry;
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
  void parse_positive_dockerHubNamespaceOnly() {
    var result = parser.parse("docker-hub::folioorg");

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(DockerHubArtifactRegistry.class);
    assertThat(result.get().getNamespace()).isEqualTo("folioorg");
    assertThat(result.get().getBaseUrl()).isEqualTo("https://hub.docker.com/v2/repositories");
  }

  @Test
  void parse_positive_dockerHubBaseUrlAndNamespace() {
    var result = parser.parse("docker-hub::https://custom-registry.io::custom-namespace");

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(DockerHubArtifactRegistry.class);
    assertThat(result.get().getNamespace()).isEqualTo("custom-namespace");
    assertThat(result.get().getBaseUrl()).isEqualTo("https://custom-registry.io");
  }

  @Test
  void parse_positive_dockerHubTrailingSlashRemoved() {
    var result = parser.parse("docker-hub::https://custom-registry.io/::namespace");

    assertThat(result).isPresent();
    assertThat(result.get().getBaseUrl()).isEqualTo("https://custom-registry.io");
  }

  @Test
  void parse_positive_folioNpmNamespaceOnly() {
    var result = parser.parse("folio-npm::npm-folio");

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(FolioNpmArtifactRegistry.class);
    assertThat(result.get().getNamespace()).isEqualTo("npm-folio");
    assertThat(result.get().getBaseUrl()).isEqualTo("https://repository.folio.org/repository");
  }

  @Test
  void parse_positive_folioNpmBaseUrlAndNamespace() {
    var result = parser.parse("folio-npm::https://custom-npm.io::custom-repo");

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(FolioNpmArtifactRegistry.class);
    assertThat(result.get().getNamespace()).isEqualTo("custom-repo");
    assertThat(result.get().getBaseUrl()).isEqualTo("https://custom-npm.io");
  }

  @Test
  void parse_positive_ecrUrlOnly() {
    var result = parser.parse("aws-ecr::https://123456789012.dkr.ecr.us-west-2.amazonaws.com");

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(EcrArtifactRegistry.class);
    assertThat(result.get().getBaseUrl()).isEqualTo("https://123456789012.dkr.ecr.us-west-2.amazonaws.com");
    assertThat(result.get().getNamespace()).isNull();
  }

  @Test
  void parse_positive_ecrUrlAndNamespace() {
    var result = parser.parse("aws-ecr::https://123456789012.dkr.ecr.us-west-2.amazonaws.com::folio");

    assertThat(result).isPresent();
    assertThat(result.get()).isInstanceOf(EcrArtifactRegistry.class);
    assertThat(result.get().getBaseUrl()).isEqualTo("https://123456789012.dkr.ecr.us-west-2.amazonaws.com");
    assertThat(result.get().getNamespace()).isEqualTo("folio");
  }

  @Test
  void parse_negative_blankString() {
    var result = parser.parse("  ");
    assertThat(result).isEmpty();
  }

  @Test
  void parse_negative_nullString() {
    var result = parser.parse(null);
    assertThat(result).isEmpty();
  }

  @Test
  void parse_negative_noPrefix() {
    var result = parser.parse("folioorg");
    assertThat(result).isEmpty();
  }

  @Test
  void parse_negative_unknownPrefix() {
    var result = parser.parse("github::folio-org");
    assertThat(result).isEmpty();
  }

  @Test
  void parse_negative_dockerHubInvalidUrl() {
    assertThatThrownBy(() -> parser.parse("docker-hub::invalid-url::namespace"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid url provided");
  }

  @Test
  void parse_negative_ecrInvalidUrl() {
    assertThatThrownBy(() -> parser.parse("aws-ecr::invalid-url"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid url provided");
  }

  @Test
  void parse_positive_dockerHubTrimmedNamespace() {
    var result = parser.parse("docker-hub::  folioorg  ");

    assertThat(result).isPresent();
    assertThat(result.get().getNamespace()).isEqualTo("folioorg");
  }

  @Test
  void parse_positive_folioNpmTrimmedNamespace() {
    var result = parser.parse("folio-npm::  npm-folio  ");

    assertThat(result).isPresent();
    assertThat(result.get().getNamespace()).isEqualTo("npm-folio");
  }
}
