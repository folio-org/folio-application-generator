package org.folio.app.generator.model.registry.artifact;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ArtifactRegistriesTest {

  @Test
  void getRegistries_positive_bePreRelease() {
    var bePreReleaseReg = new DockerHubArtifactRegistry().namespace("folioci");
    var beReg = new DockerHubArtifactRegistry().namespace("folioorg");
    var unified = new DockerHubArtifactRegistry().namespace("unified");

    var registries = new ArtifactRegistries(
      List.of(beReg), List.of(), List.of(bePreReleaseReg), List.of(), List.of(unified));

    var result = registries.getRegistries(ModuleType.BE, true);

    assertThat(result).containsExactly(bePreReleaseReg, beReg, unified);
  }

  @Test
  void getRegistries_positive_beStable() {
    var beReg = new DockerHubArtifactRegistry().namespace("folioorg");
    var unified = new DockerHubArtifactRegistry().namespace("unified");

    var registries = new ArtifactRegistries(
      List.of(beReg), List.of(), List.of(), List.of(), List.of(unified));

    var result = registries.getRegistries(ModuleType.BE, false);

    assertThat(result).containsExactly(beReg, unified);
  }

  @Test
  void getRegistries_positive_uiPreRelease() {
    var uiPreReleaseReg = new FolioNpmArtifactRegistry().namespace("npm-folioci");
    var uiReg = new FolioNpmArtifactRegistry().namespace("npm-folio");

    var registries = new ArtifactRegistries(
      List.of(), List.of(uiReg), List.of(), List.of(uiPreReleaseReg), List.of());

    var result = registries.getRegistries(ModuleType.UI, true);

    assertThat(result).containsExactly(uiPreReleaseReg, uiReg);
  }

  @Test
  void getRegistries_positive_nullPreReleaseRegistries() {
    var beReg = new DockerHubArtifactRegistry().namespace("folioorg");

    var registries = new ArtifactRegistries(List.of(beReg), List.of(), null, null, null);

    var result = registries.getRegistries(ModuleType.BE, true);

    assertThat(result).containsExactly(beReg);
  }

  @Test
  void getRegistries_positive_emptyPreReleaseRegistries() {
    var beReg = new DockerHubArtifactRegistry().namespace("folioorg");

    var registries = new ArtifactRegistries(List.of(beReg), List.of(), List.of(), List.of(), List.of());

    var result = registries.getRegistries(ModuleType.BE, true);

    assertThat(result).containsExactly(beReg);
  }

  @Test
  void getRegistries_positive_nullTypeRegistries() {
    var unified = new DockerHubArtifactRegistry().namespace("unified");

    var registries = new ArtifactRegistries(null, null, null, null, List.of(unified));

    var result = registries.getRegistries(ModuleType.BE, false);

    assertThat(result).containsExactly(unified);
  }

  @Test
  void getRegistries_positive_allNull() {
    var registries = new ArtifactRegistries(null, null, null, null, null);

    var result = registries.getRegistries(ModuleType.BE, false);

    assertThat(result).isEmpty();
  }
}
