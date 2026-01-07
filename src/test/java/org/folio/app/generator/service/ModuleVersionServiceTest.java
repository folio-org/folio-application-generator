package org.folio.app.generator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.PreReleaseFilter;
import org.folio.app.generator.model.registry.ModuleRegistries;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.registry.S3ModuleRegistry;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistries;
import org.folio.app.generator.model.registry.artifact.DockerHubArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.FolioNpmArtifactRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.service.artifact.existence.ArtifactExistenceCheckerFacade;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.service.resolver.ModuleVersionResolverFacade;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.PluginConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ModuleVersionServiceTest {

  @Mock private Log log;
  @Mock private ModuleRegistries moduleRegistries;
  @Mock private ModuleVersionResolverFacade resolverFacade;
  @Mock private PluginConfig pluginConfig;
  @Mock private ArtifactExistenceCheckerFacade artifactExistenceCheckerFacade;
  @Mock private ArtifactRegistryProvider artifactRegistryProvider;

  private ModuleVersionService service;

  @BeforeEach
  void setUp() {
    service = new ModuleVersionService(log, moduleRegistries, resolverFacade,
      pluginConfig, artifactExistenceCheckerFacade, artifactRegistryProvider);
    lenient().when(pluginConfig.isValidateArtifacts()).thenReturn(false);
  }

  @Test
  void resolveModulesConstraints_positive()  {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.2.0", "1.1.0", "1.0.0")));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("mod-foo");
    assertThat(result.get(0).getVersion()).isEqualTo("1.2.0");
  }

  @Test
  void resolveModulesConstraints_positive_exactVersion()  {
    var dependency = new Dependency("mod-foo", "1.2.0", PreReleaseFilter.FALSE);

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(okapiRegistry()));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("mod-foo");
    assertThat(result.get(0).getVersion()).isEqualTo("1.2.0");
  }

  @Test
  void resolveModulesConstraints_positive_tildeConstraint()  {
    var dependency = new Dependency("mod-foo", "~1.2.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.3.0", "1.2.5", "1.2.0", "1.1.0")));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.2.5");
  }

  @Test
  void resolveModulesConstraints_positive_greaterOrEqualConstraint()  {
    var dependency = new Dependency("mod-foo", ">=1.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("2.0.0", "1.5.0", "1.0.0")));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("2.0.0");
  }

  @Test
  void resolveModulesConstraints_positive_emptyRegistries()  {
    var dependency = new Dependency("mod-foo", "1.0.0", PreReleaseFilter.FALSE);

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of());

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    verify(log).warn("Module registries are empty for type: BE");
  }

  @Test
  void resolveModulesConstraints_positive_selectsGreatestVersionAcrossRegistries()  {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var okapiReg = okapiRegistry();
    var s3Reg = s3Registry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(okapiReg, s3Reg));
    when(resolverFacade.getAvailableVersions(okapiReg, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.1.0", "1.0.0")));
    when(resolverFacade.getAvailableVersions(s3Reg, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.2.0", "1.0.5")));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.2.0");
  }

  @Test
  void resolveModulesConstraints_positive_multipleDependencies()  {
    var dep1 = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var dep2 = new Dependency("mod-bar", "2.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dep1, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.5.0", "1.0.0")));

    var result = service.resolveModulesConstraints(List.of(dep1, dep2), ModuleType.BE);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getVersion()).isEqualTo("1.5.0");
    assertThat(result.get(1).getVersion()).isEqualTo("2.0.0");
  }

  @Test
  void resolveModulesConstraints_positive_preReleaseTrue()  {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.TRUE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.2.0-SNAPSHOT", "1.1.0", "1.0.0")));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.2.0-SNAPSHOT");
  }

  @Test
  void resolveModulesConstraints_positive_preReleaseNull()  {
    var dependency = new Dependency("mod-foo", "^1.0.0", null);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.2.0-SNAPSHOT", "1.1.0", "1.0.0")));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.2.0-SNAPSHOT");
    assertThat(result.get(0).getPreRelease()).isNull();
  }

  @Test
  void resolveModulesConstraints_positive_preReleaseOnly()  {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.ONLY);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.2.0-alpha", "1.1.0-beta")));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.2.0-alpha");
  }

  @Test
  void resolveModulesConstraints_positive_uiModule()  {
    var dependency = new Dependency("folio_app", "^1.0.0", PreReleaseFilter.TRUE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.UI)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.UI))
        .thenReturn(Optional.of(List.of("1.1.10010000000123", "1.0.5")));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.UI);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.1.10010000000123");
  }

  @Test
  void resolveModulesConstraints_negative_noMatchingVersion() {
    var dependency = new Dependency("mod-foo", "^5.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.2.0", "1.1.0")));

    assertThatThrownBy(() -> service.resolveModulesConstraints(List.of(dependency), ModuleType.BE))
        .isInstanceOf(ApplicationGeneratorException.class)
        .hasMessage("No version matching constraint '^5.0.0' found for BE module 'mod-foo' in any registry");
  }

  @Test
  void resolveModulesConstraints_negative_moduleNotFoundInRegistry() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.resolveModulesConstraints(List.of(dependency), ModuleType.BE))
        .isInstanceOf(ApplicationGeneratorException.class)
        .hasMessage("No version matching constraint '^1.0.0' found for BE module 'mod-foo' in any registry");
  }

  @Test
  void resolveModulesConstraints_positive_continuesOnRegistryException()  {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var okapiReg = okapiRegistry();
    var s3Reg = s3Registry();
    var exception = new RuntimeException("Connection failed");

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(okapiReg, s3Reg));
    when(resolverFacade.getAvailableVersions(okapiReg, dependency, ModuleType.BE)).thenThrow(exception);
    when(resolverFacade.getAvailableVersions(s3Reg, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.2.0")));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.2.0");
    verify(log).warn(eq("Failed to resolve constraint '^1.0.0' for module 'mod-foo' from OkapiModuleRegistry"),
        any(RuntimeException.class));
  }

  @Test
  void resolveModulesConstraints_negative_invalidConstraint() {
    var dependency = new Dependency("mod-foo", "invalid", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.2.0", "1.1.0")));

    assertThatThrownBy(() -> service.resolveModulesConstraints(List.of(dependency), ModuleType.BE))
        .isInstanceOf(ApplicationGeneratorException.class)
        .hasMessage("No version matching constraint 'invalid' found for BE module 'mod-foo' in any registry");

    verify(log).warn("Invalid version constraint 'invalid' for module 'mod-foo'");
  }

  @Test
  void resolveModulesConstraints_positive_preservesPreReleaseFilter()  {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.ONLY);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.1.0-SNAPSHOT")));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPreRelease()).isEqualTo(PreReleaseFilter.ONLY);
  }

  @Test
  void resolveModulesConstraints_negative_emptyVersionsList() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of()));

    assertThatThrownBy(() -> service.resolveModulesConstraints(List.of(dependency), ModuleType.BE))
        .isInstanceOf(ApplicationGeneratorException.class)
        .hasMessage("No version matching constraint '^1.0.0' found for BE module 'mod-foo' in any registry");
  }

  @Test
  void resolveModulesConstraints_negative_noVersionMatchesConstraint() {
    var dependency = new Dependency("mod-foo", "^2.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.0.0", "1.5.0")));

    assertThatThrownBy(() -> service.resolveModulesConstraints(List.of(dependency), ModuleType.BE))
        .isInstanceOf(ApplicationGeneratorException.class)
        .hasMessage("No version matching constraint '^2.0.0' found for BE module 'mod-foo' in any registry");
  }

  @Test
  void resolveModulesConstraints_positive_filtersInvalidSemverVersions()  {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(Arrays.asList(
            "1.5.0", "invalid-version", "1.2.3a", "1.2.0", "bad.version", "1.0.0", null)));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.5.0");
  }

  @Test
  void resolveModulesConstraints_positive_versionWithBuildMetadata()  {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.5.0+build.123", "1.2.0", "1.0.0")));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.5.0+build.123");
  }

  @Test
  void resolveModulesConstraints_positive_fallbackToNextVersionWhenArtifactNotFound() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();
    var artifactRegistries = createDefaultArtifactRegistries();

    when(pluginConfig.isValidateArtifacts()).thenReturn(true);
    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.5.0", "1.4.0", "1.3.0", "1.0.0")));
    when(artifactRegistryProvider.getArtifactRegistries(pluginConfig)).thenReturn(artifactRegistries);
    when(artifactExistenceCheckerFacade.exists(any(), any(), eq(ModuleType.BE)))
        .thenReturn(false)
        .thenReturn(false)
        .thenReturn(true);

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.3.0");
  }

  @Test
  void resolveModulesConstraints_positive_skipArtifactValidationWhenDisabled() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();

    when(pluginConfig.isValidateArtifacts()).thenReturn(false);
    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.5.0", "1.4.0", "1.0.0")));

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.5.0");
    verify(artifactExistenceCheckerFacade, never()).exists(any(), any(), any());
  }

  @Test
  void resolveModulesConstraints_negative_noVersionWithArtifactFound() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();
    var artifactRegistries = createDefaultArtifactRegistries();

    when(pluginConfig.isValidateArtifacts()).thenReturn(true);
    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.5.0", "1.4.0")));
    when(artifactRegistryProvider.getArtifactRegistries(pluginConfig)).thenReturn(artifactRegistries);
    when(artifactExistenceCheckerFacade.exists(any(), any(), eq(ModuleType.BE))).thenReturn(false);

    assertThatThrownBy(() -> service.resolveModulesConstraints(List.of(dependency), ModuleType.BE))
        .isInstanceOf(ApplicationGeneratorException.class)
        .hasMessage("No version matching constraint '^1.0.0' found for BE module 'mod-foo' in any registry");
  }

  @Test
  void resolveModulesConstraints_positive_snapshotVersionUsesSnapshotRegistry() {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.TRUE);
    var registry = okapiRegistry();
    var artifactRegistries = createDefaultArtifactRegistries();

    when(pluginConfig.isValidateArtifacts()).thenReturn(true);
    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.BE))
        .thenReturn(Optional.of(List.of("1.5.0-SNAPSHOT.123")));
    when(artifactRegistryProvider.getArtifactRegistries(pluginConfig)).thenReturn(artifactRegistries);
    when(artifactExistenceCheckerFacade.exists(any(), any(), eq(ModuleType.BE))).thenReturn(true);

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.BE);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.5.0-SNAPSHOT.123");
  }

  @Test
  void resolveModulesConstraints_positive_uiModuleArtifactValidation()  {
    var dependency = new Dependency("folio_users", "^1.0.0", PreReleaseFilter.FALSE);
    var registry = okapiRegistry();
    var artifactRegistries = createDefaultArtifactRegistries();

    when(pluginConfig.isValidateArtifacts()).thenReturn(true);
    when(moduleRegistries.getRegistries(ModuleType.UI)).thenReturn(List.of(registry));
    when(resolverFacade.getAvailableVersions(registry, dependency, ModuleType.UI))
        .thenReturn(Optional.of(List.of("1.5.0", "1.4.0")));
    when(artifactRegistryProvider.getArtifactRegistries(pluginConfig)).thenReturn(artifactRegistries);
    when(artifactExistenceCheckerFacade.exists(any(), any(), eq(ModuleType.UI)))
        .thenReturn(false)
        .thenReturn(true);

    var result = service.resolveModulesConstraints(List.of(dependency), ModuleType.UI);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo("1.4.0");
  }

  private static OkapiModuleRegistry okapiRegistry() {
    return new OkapiModuleRegistry().url("http://localhost").withGeneratedFields();
  }

  private static S3ModuleRegistry s3Registry() {
    return new S3ModuleRegistry().bucket("test-bucket").path("modules/").withGeneratedFields();
  }

  private static ArtifactRegistries createDefaultArtifactRegistries() {
    var beRegistries = List.of(
      (org.folio.app.generator.model.registry.artifact.ArtifactRegistry)
        new DockerHubArtifactRegistry().namespace("folioorg")
    );
    var bePreReleaseRegistries = List.of(
      (org.folio.app.generator.model.registry.artifact.ArtifactRegistry)
        new DockerHubArtifactRegistry().namespace("folioci")
    );
    var uiRegistries = List.of(
      (org.folio.app.generator.model.registry.artifact.ArtifactRegistry)
        new FolioNpmArtifactRegistry().namespace("npm-folio")
    );
    var uiPreReleaseRegistries = List.of(
      (org.folio.app.generator.model.registry.artifact.ArtifactRegistry)
        new FolioNpmArtifactRegistry().namespace("npm-folioci")
    );
    return new ArtifactRegistries(
      beRegistries, uiRegistries, bePreReleaseRegistries, uiPreReleaseRegistries, List.of());
  }
}
