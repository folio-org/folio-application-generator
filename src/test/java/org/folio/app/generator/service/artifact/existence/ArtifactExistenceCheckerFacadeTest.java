package org.folio.app.generator.service.artifact.existence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.DockerHubArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.EcrArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.FolioNpmArtifactRegistry;
import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ArtifactExistenceCheckerFacadeTest {

  @Mock private ArtifactExistenceChecker dockerHubChecker;
  @Mock private ArtifactExistenceChecker folioNpmChecker;
  @Mock private ArtifactExistenceChecker ecrChecker;

  private ArtifactExistenceCheckerFacade facade;

  @BeforeEach
  void setUp() {
    when(dockerHubChecker.getRegistryType()).thenReturn(ArtifactRegistryType.DOCKER_HUB);
    when(folioNpmChecker.getRegistryType()).thenReturn(ArtifactRegistryType.FOLIO_NPM);
    when(ecrChecker.getRegistryType()).thenReturn(ArtifactRegistryType.AWS_ECR);
    facade = new ArtifactExistenceCheckerFacade(List.of(dockerHubChecker, folioNpmChecker, ecrChecker));
  }

  @Test
  void exists_positive_dockerHub() {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    when(dockerHubChecker.exists(module, registry)).thenReturn(true);

    var result = facade.exists(module, registry);

    assertThat(result).isTrue();
    verify(dockerHubChecker).exists(module, registry);
  }

  @Test
  void exists_positive_folioNpm() {
    var module = new ModuleDefinition().name("folio_users").version("1.0.0");
    var registry = new FolioNpmArtifactRegistry().namespace("npm-folio");

    when(folioNpmChecker.exists(module, registry)).thenReturn(true);

    var result = facade.exists(module, registry);

    assertThat(result).isTrue();
    verify(folioNpmChecker).exists(module, registry);
  }

  @Test
  void exists_positive_ecr() {
    var module = new ModuleDefinition().name("mod-users").version("1.2.0-SNAPSHOT.5dc446");
    var registry = new EcrArtifactRegistry()
      .baseUrl("https://123456789012.dkr.ecr.us-west-2.amazonaws.com")
      .namespace("folio");

    when(ecrChecker.exists(module, registry)).thenReturn(true);

    var result = facade.exists(module, registry);

    assertThat(result).isTrue();
    verify(ecrChecker).exists(module, registry);
  }

  @Test
  void exists_negative_artifactNotFound() {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    when(dockerHubChecker.exists(module, registry)).thenReturn(false);

    var result = facade.exists(module, registry);

    assertThat(result).isFalse();
  }

  @Test
  void exists_negative_unknownRegistryType() {
    var module = new ModuleDefinition().name("folio_users").version("1.0.0");
    var registry = new FolioNpmArtifactRegistry().namespace("npm-folio");

    var facadeWithLimitedCheckers = new ArtifactExistenceCheckerFacade(List.of(dockerHubChecker));

    assertThatThrownBy(() -> facadeWithLimitedCheckers.exists(module, registry))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("No artifact existence checker found for registry type: FOLIO_NPM")
      .hasMessageContaining("configuration or programming error");
  }
}
