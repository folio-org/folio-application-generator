package org.folio.app.generator.service.artifact.existence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.DockerHubArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.FolioNpmArtifactRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ArtifactExistenceCheckerFacadeTest {

  @Mock private Log log;
  @Mock private ArtifactExistenceChecker beChecker;
  @Mock private ArtifactExistenceChecker uiChecker;

  private ArtifactExistenceCheckerFacade facade;

  @BeforeEach
  void setUp() {
    when(beChecker.getModuleType()).thenReturn(ModuleType.BE);
    when(uiChecker.getModuleType()).thenReturn(ModuleType.UI);
    facade = new ArtifactExistenceCheckerFacade(log, List.of(beChecker, uiChecker));
  }

  @Test
  void exists_positive_beModule() {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    when(beChecker.exists(module, registry)).thenReturn(true);

    var result = facade.exists(module, registry, ModuleType.BE);

    assertThat(result).isTrue();
    verify(beChecker).exists(module, registry);
  }

  @Test
  void exists_positive_uiModule() {
    var module = new ModuleDefinition().name("folio_users").version("1.0.0");
    var registry = new FolioNpmArtifactRegistry().namespace("npm-folio");

    when(uiChecker.exists(module, registry)).thenReturn(true);

    var result = facade.exists(module, registry, ModuleType.UI);

    assertThat(result).isTrue();
    verify(uiChecker).exists(module, registry);
  }

  @Test
  void exists_negative_artifactNotFound() {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    when(beChecker.exists(module, registry)).thenReturn(false);

    var result = facade.exists(module, registry, ModuleType.BE);

    assertThat(result).isFalse();
  }

  @Test
  void exists_negative_unknownModuleType() {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    var facadeWithLimitedCheckers = new ArtifactExistenceCheckerFacade(log, List.of(beChecker));

    assertThatThrownBy(() -> facadeWithLimitedCheckers.exists(module, registry, ModuleType.UI))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("No artifact existence checker found for module type: UI")
      .hasMessageContaining("configuration or programming error");
  }
}
