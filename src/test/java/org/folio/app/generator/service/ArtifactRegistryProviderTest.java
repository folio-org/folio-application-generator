package org.folio.app.generator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.folio.app.generator.model.registry.artifact.ConfigArtifactRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.service.parsers.StringArtifactRegistryParser;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.PluginConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class ArtifactRegistryProviderTest {

  private ArtifactRegistryProvider artifactRegistryProvider;

  @BeforeEach
  void setUp() {
    this.artifactRegistryProvider = new ArtifactRegistryProvider(new StringArtifactRegistryParser());
  }

  @Test
  void getArtifactRegistries_positive_defaultRegistries() {
    var config = PluginConfig.builder().build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.beRegistries()).hasSize(1);
    assertThat(result.beRegistries().get(0).getNamespace()).isEqualTo("folioorg");
    assertThat(result.bePreReleaseRegistries()).hasSize(1);
    assertThat(result.bePreReleaseRegistries().get(0).getNamespace()).isEqualTo("folioci");
    assertThat(result.uiRegistries()).hasSize(1);
    assertThat(result.uiRegistries().get(0).getNamespace()).isEqualTo("npm-folio");
    assertThat(result.uiPreReleaseRegistries()).hasSize(1);
    assertThat(result.uiPreReleaseRegistries().get(0).getNamespace()).isEqualTo("npm-folioci");
    assertThat(result.unifiedRegistries()).isEmpty();
  }

  @Test
  void getArtifactRegistries_positive_customBeRegistryFromConfig() {
    var beRegistry = new ConfigArtifactRegistry();
    beRegistry.setType("docker-hub");
    beRegistry.setNamespace("custom-namespace");
    var config = PluginConfig.builder()
      .beArtifactRegistries(List.of(beRegistry))
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.beRegistries()).hasSize(1);
    assertThat(result.beRegistries().get(0).getNamespace()).isEqualTo("custom-namespace");
    assertThat(result.bePreReleaseRegistries()).isEmpty();
    assertThat(result.uiRegistries()).hasSize(1);
    assertThat(result.uiPreReleaseRegistries()).hasSize(1);
  }

  @Test
  void getArtifactRegistries_positive_customUiRegistryFromConfig() {
    var uiRegistry = new ConfigArtifactRegistry();
    uiRegistry.setType("folio-npm");
    uiRegistry.setNamespace("custom-npm");
    var config = PluginConfig.builder()
      .uiArtifactRegistries(List.of(uiRegistry))
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.uiRegistries()).hasSize(1);
    assertThat(result.uiRegistries().get(0).getNamespace()).isEqualTo("custom-npm");
    assertThat(result.uiPreReleaseRegistries()).isEmpty();
    assertThat(result.beRegistries()).hasSize(1);
    assertThat(result.bePreReleaseRegistries()).hasSize(1);
  }

  @Test
  void getArtifactRegistries_positive_customBeRegistryFromCommandLine() {
    var config = PluginConfig.builder()
      .cmdBeArtifactRegistries("my-namespace")
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.beRegistries()).hasSize(1);
    assertThat(result.beRegistries().get(0).getNamespace()).isEqualTo("my-namespace");
    assertThat(result.bePreReleaseRegistries()).isEmpty();
  }

  @Test
  void getArtifactRegistries_positive_customUiRegistryFromCommandLine() {
    var config = PluginConfig.builder()
      .cmdUiArtifactRegistries("my-npm-repo")
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.uiRegistries()).hasSize(1);
    assertThat(result.uiRegistries().get(0).getNamespace()).isEqualTo("my-npm-repo");
    assertThat(result.uiPreReleaseRegistries()).isEmpty();
  }

  @Test
  void getArtifactRegistries_positive_customRegistryWithUrl() {
    var config = PluginConfig.builder()
      .cmdBeArtifactRegistries("https://custom-registry.io::custom-ns")
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.beRegistries()).hasSize(1);
    assertThat(result.beRegistries().get(0).getBaseUrl()).isEqualTo("https://custom-registry.io");
    assertThat(result.beRegistries().get(0).getNamespace()).isEqualTo("custom-ns");
  }

  @Test
  void getArtifactRegistries_positive_multipleCommandLineRegistries() {
    var config = PluginConfig.builder()
      .cmdBeArtifactRegistries("namespace1,namespace2,namespace3")
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.beRegistries()).hasSize(3);
    assertThat(result.beRegistries().get(0).getNamespace()).isEqualTo("namespace1");
    assertThat(result.beRegistries().get(1).getNamespace()).isEqualTo("namespace2");
    assertThat(result.beRegistries().get(2).getNamespace()).isEqualTo("namespace3");
  }

  @Test
  void getArtifactRegistries_positive_combineCommandLineAndConfigRegistries() {
    var beRegistry = new ConfigArtifactRegistry();
    beRegistry.setType("docker-hub");
    beRegistry.setNamespace("config-namespace");
    var config = PluginConfig.builder()
      .cmdBeArtifactRegistries("cmd-namespace")
      .beArtifactRegistries(List.of(beRegistry))
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.beRegistries()).hasSize(2);
    assertThat(result.beRegistries().get(0).getNamespace()).isEqualTo("cmd-namespace");
    assertThat(result.beRegistries().get(1).getNamespace()).isEqualTo("config-namespace");
  }

  @Test
  void getArtifactRegistries_negative_invalidConfigRegistryType() {
    var invalidRegistry = new ConfigArtifactRegistry();
    invalidRegistry.setType("invalid-type");
    invalidRegistry.setNamespace("test");
    var config = PluginConfig.builder()
      .beArtifactRegistries(List.of(invalidRegistry))
      .build();

    assertThatThrownBy(() -> artifactRegistryProvider.getArtifactRegistries(config))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid artifact registries found");
  }

  @Test
  void getArtifactRegistries_positive_registryWithCustomBaseUrl() {
    var beRegistry = new ConfigArtifactRegistry();
    beRegistry.setType("docker-hub");
    beRegistry.setNamespace("custom-ns");
    beRegistry.setBaseUrl("https://private-registry.io/v2");
    var config = PluginConfig.builder()
      .beArtifactRegistries(List.of(beRegistry))
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.beRegistries()).hasSize(1);
    assertThat(result.beRegistries().get(0).getBaseUrl()).isEqualTo("https://private-registry.io/v2");
    assertThat(result.beRegistries().get(0).getNamespace()).isEqualTo("custom-ns");
  }

  @Test
  void getArtifactRegistries_positive_getRegistriesByModuleTypeAndReleaseVersion() {
    var config = PluginConfig.builder().build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    var beReleaseRegistries = result.getRegistries(ModuleType.BE, false);
    var uiReleaseRegistries = result.getRegistries(ModuleType.UI, false);

    assertThat(beReleaseRegistries).hasSize(1);
    assertThat(beReleaseRegistries.get(0).getNamespace()).isEqualTo("folioorg");
    assertThat(uiReleaseRegistries).hasSize(1);
    assertThat(uiReleaseRegistries.get(0).getNamespace()).isEqualTo("npm-folio");
  }

  @Test
  void getArtifactRegistries_positive_getRegistriesByModuleTypeAndPreReleaseVersion() {
    var config = PluginConfig.builder().build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    var bePreReleaseRegistries = result.getRegistries(ModuleType.BE, true);
    var uiPreReleaseRegistries = result.getRegistries(ModuleType.UI, true);

    assertThat(bePreReleaseRegistries).hasSize(2);
    assertThat(bePreReleaseRegistries.get(0).getNamespace()).isEqualTo("folioci");
    assertThat(bePreReleaseRegistries.get(1).getNamespace()).isEqualTo("folioorg");
    assertThat(uiPreReleaseRegistries).hasSize(2);
    assertThat(uiPreReleaseRegistries.get(0).getNamespace()).isEqualTo("npm-folioci");
    assertThat(uiPreReleaseRegistries.get(1).getNamespace()).isEqualTo("npm-folio");
  }

  @Test
  void getArtifactRegistries_positive_bePreReleaseRegistriesFromConfig() {
    var preReleaseRegistry = new ConfigArtifactRegistry();
    preReleaseRegistry.setType("docker-hub");
    preReleaseRegistry.setNamespace("my-snapshots");
    var config = PluginConfig.builder()
      .bePreReleaseArtifactRegistries(List.of(preReleaseRegistry))
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.bePreReleaseRegistries()).hasSize(1);
    assertThat(result.bePreReleaseRegistries().get(0).getNamespace()).isEqualTo("my-snapshots");
    assertThat(result.beRegistries()).isEmpty();
  }

  @Test
  void getArtifactRegistries_positive_uiPreReleaseRegistriesFromConfig() {
    var preReleaseRegistry = new ConfigArtifactRegistry();
    preReleaseRegistry.setType("folio-npm");
    preReleaseRegistry.setNamespace("npm-snapshots");
    var config = PluginConfig.builder()
      .uiPreReleaseArtifactRegistries(List.of(preReleaseRegistry))
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.uiPreReleaseRegistries()).hasSize(1);
    assertThat(result.uiPreReleaseRegistries().get(0).getNamespace()).isEqualTo("npm-snapshots");
    assertThat(result.uiRegistries()).isEmpty();
  }

  @Test
  void getArtifactRegistries_positive_preReleaseRegistriesFromCommandLine() {
    var config = PluginConfig.builder()
      .cmdBePreReleaseArtifactRegistries("snapshot-ns")
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.bePreReleaseRegistries()).hasSize(1);
    assertThat(result.bePreReleaseRegistries().get(0).getNamespace()).isEqualTo("snapshot-ns");
  }

  @Test
  void getArtifactRegistries_positive_unifiedRegistriesFromConfig() {
    var dockerRegistry = new ConfigArtifactRegistry();
    dockerRegistry.setType("docker-hub");
    dockerRegistry.setNamespace("unified-docker");

    var npmRegistry = new ConfigArtifactRegistry();
    npmRegistry.setType("folio-npm");
    npmRegistry.setNamespace("unified-npm");

    var config = PluginConfig.builder()
      .artifactRegistries(List.of(dockerRegistry, npmRegistry))
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.unifiedRegistries()).hasSize(2);
    assertThat(result.unifiedRegistries().get(0).getNamespace()).isEqualTo("unified-docker");
    assertThat(result.unifiedRegistries().get(1).getNamespace()).isEqualTo("unified-npm");
    assertThat(result.beRegistries()).isEmpty();
    assertThat(result.uiRegistries()).isEmpty();
  }

  @Test
  void getArtifactRegistries_positive_unifiedRegistriesFallback() {
    var dockerRegistry = new ConfigArtifactRegistry();
    dockerRegistry.setType("docker-hub");
    dockerRegistry.setNamespace("unified-docker");

    var config = PluginConfig.builder()
      .artifactRegistries(List.of(dockerRegistry))
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    var beRegistries = result.getRegistries(ModuleType.BE, false);
    assertThat(beRegistries).hasSize(1);
    assertThat(beRegistries.get(0).getNamespace()).isEqualTo("unified-docker");
  }

  @Test
  void getArtifactRegistries_positive_resolutionOrder() {
    var bePreRelease = new ConfigArtifactRegistry();
    bePreRelease.setType("docker-hub");
    bePreRelease.setNamespace("pre-release-ns");

    var beRelease = new ConfigArtifactRegistry();
    beRelease.setType("docker-hub");
    beRelease.setNamespace("release-ns");

    var unified = new ConfigArtifactRegistry();
    unified.setType("docker-hub");
    unified.setNamespace("unified-ns");

    var config = PluginConfig.builder()
      .bePreReleaseArtifactRegistries(List.of(bePreRelease))
      .beArtifactRegistries(List.of(beRelease))
      .artifactRegistries(List.of(unified))
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    var preReleaseRegistries = result.getRegistries(ModuleType.BE, true);
    assertThat(preReleaseRegistries).hasSize(3);
    assertThat(preReleaseRegistries.get(0).getNamespace()).isEqualTo("pre-release-ns");
    assertThat(preReleaseRegistries.get(1).getNamespace()).isEqualTo("release-ns");
    assertThat(preReleaseRegistries.get(2).getNamespace()).isEqualTo("unified-ns");

    var releaseRegistries = result.getRegistries(ModuleType.BE, false);
    assertThat(releaseRegistries).hasSize(2);
    assertThat(releaseRegistries.get(0).getNamespace()).isEqualTo("release-ns");
    assertThat(releaseRegistries.get(1).getNamespace()).isEqualTo("unified-ns");
  }

  @Test
  void getArtifactRegistries_positive_emptyConfigRegistries() {
    var config = PluginConfig.builder()
      .beArtifactRegistries(List.of())
      .uiArtifactRegistries(List.of())
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.beRegistries()).hasSize(1);
    assertThat(result.beRegistries().get(0).getNamespace()).isEqualTo("folioorg");
  }

  @Test
  void getArtifactRegistries_negative_invalidRegistryNamespace() {
    var invalidRegistry = new ConfigArtifactRegistry();
    invalidRegistry.setType("docker-hub");
    invalidRegistry.setNamespace("");
    var config = PluginConfig.builder()
      .beArtifactRegistries(List.of(invalidRegistry))
      .build();

    assertThatThrownBy(() -> artifactRegistryProvider.getArtifactRegistries(config))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Invalid artifact registries found");
  }

  @Test
  void getArtifactRegistries_positive_npmRegistryWithCustomBaseUrl() {
    var npmRegistry = new ConfigArtifactRegistry();
    npmRegistry.setType("folio-npm");
    npmRegistry.setNamespace("custom-npm");
    npmRegistry.setBaseUrl("https://private-npm.io/repository");
    var config = PluginConfig.builder()
      .uiArtifactRegistries(List.of(npmRegistry))
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.uiRegistries()).hasSize(1);
    assertThat(result.uiRegistries().get(0).getBaseUrl()).isEqualTo("https://private-npm.io/repository");
    assertThat(result.uiRegistries().get(0).getNamespace()).isEqualTo("custom-npm");
  }

  @Test
  void getArtifactRegistries_positive_unifiedRegistriesFromCommandLine() {
    var config = PluginConfig.builder()
      .cmdArtifactRegistries("docker-ns,npm-ns")
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.unifiedRegistries()).hasSize(2);
    assertThat(result.beRegistries()).isEmpty();
    assertThat(result.uiRegistries()).isEmpty();
  }

  @Test
  void getArtifactRegistries_positive_emptyUnifiedConfigRegistries() {
    var config = PluginConfig.builder()
      .artifactRegistries(List.of())
      .build();

    var result = artifactRegistryProvider.getArtifactRegistries(config);

    assertThat(result.unifiedRegistries()).isEmpty();
    assertThat(result.beRegistries()).hasSize(1);
    assertThat(result.uiRegistries()).hasSize(1);
  }
}
