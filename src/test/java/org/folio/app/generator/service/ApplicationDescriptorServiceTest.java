package org.folio.app.generator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.app.generator.model.types.ModuleType.BE;
import static org.folio.app.generator.model.types.ModuleType.UI;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.ModulesLoadResult;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.PluginConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ApplicationDescriptorServiceTest {

  @Mock private MavenProject mavenProject;
  @Mock private PluginConfig pluginConfig;
  @Mock private ModuleDescriptorService moduleDescriptorService;
  @Mock private ModuleVersionService moduleVersionService;
  @InjectMocks private ApplicationDescriptorService service;

  @BeforeEach
  void setUp() throws Exception {
    lenient().when(moduleVersionService.resolveModulesConstraints(anyList(), eq(BE)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(moduleVersionService.resolveModulesConstraints(anyList(), eq(UI)))
      .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @SneakyThrows
  void create_positive_withTemplateNameAndVersion() {
    var template = new ApplicationDescriptorTemplate()
      .name("test-app")
      .version("1.0.0")
      .modules(List.of(new Dependency("mod-test", "1.0.0", null)))
      .uiModules(List.of(new Dependency("folio_test", "2.0.0", null)))
      .dependencies(List.of(new Dependency("app-platform", "^1.0.0", null)));

    template.setDescription("Test application");

    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(
        List.of(new ModuleDefinition().id("mod-test-1.0.0").name("mod-test").version("1.0.0")),
        List.of(Map.of("id", "mod-test-1.0.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(
        List.of(new ModuleDefinition().id("folio_test-2.0.0").name("folio_test").version("2.0.0")),
        List.of(Map.of("id", "folio_test-2.0.0"))));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);

    var result = service.create(template);

    assertThat(result.getId()).isEqualTo("test-app-1.0.0");
    assertThat(result.getName()).isEqualTo("test-app");
    assertThat(result.getVersion()).isEqualTo("1.0.0");
    assertThat(result.getDescription()).isEqualTo("Test application");
    assertThat(result.getModules()).hasSize(1);
    assertThat(result.getUiModules()).hasSize(1);
    assertThat(result.getDependencies()).hasSize(1);
    assertThat(result.getModuleDescriptors()).isEmpty();
    assertThat(result.getUiModuleDescriptors()).isEmpty();
  }

  @Test
  @SneakyThrows
  void create_positive_withMavenProjectMetadata() {
    var template = new ApplicationDescriptorTemplate()
      .modules(List.of(new Dependency("mod-test", "1.0.0", null)));

    when(mavenProject.getName()).thenReturn("maven-app");
    when(mavenProject.getVersion()).thenReturn("2.0.0-SNAPSHOT");
    when(mavenProject.getDescription()).thenReturn("Maven project description");
    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(
        List.of(new ModuleDefinition().id("mod-test-1.0.0").name("mod-test").version("1.0.0")),
        List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);

    var result = service.create(template);

    assertThat(result.getId()).isEqualTo("maven-app-2.0.0-SNAPSHOT");
    assertThat(result.getName()).isEqualTo("maven-app");
    assertThat(result.getVersion()).isEqualTo("2.0.0-SNAPSHOT");
    assertThat(result.getDescription()).isEqualTo("Maven project description");
  }

  @Test
  @SneakyThrows
  void create_positive_withBuildNumber() {
    var template = new ApplicationDescriptorTemplate()
      .modules(List.of());

    when(mavenProject.getName()).thenReturn("maven-app");
    when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");
    when(pluginConfig.getBuildNumber()).thenReturn("12345");
    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);

    var result = service.create(template);

    assertThat(result.getId()).isEqualTo("maven-app-1.0.0-SNAPSHOT.12345");
    assertThat(result.getVersion()).isEqualTo("1.0.0-SNAPSHOT.12345");
  }

  @Test
  @SneakyThrows
  void create_positive_buildNumberNotAppliedToStableVersion() {
    var template = new ApplicationDescriptorTemplate()
      .name("test-app")
      .version("1.0.0")
      .modules(List.of());

    when(pluginConfig.getBuildNumber()).thenReturn("12345");
    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);

    var result = service.create(template);

    assertThat(result.getId()).isEqualTo("test-app-1.0.0");
    assertThat(result.getVersion()).isEqualTo("1.0.0");
  }

  @Test
  @SneakyThrows
  void create_positive_withModuleDescriptors() {
    var template = new ApplicationDescriptorTemplate()
      .name("test-app")
      .version("1.0.0")
      .modules(List.of(new Dependency("mod-test", "1.0.0", null)))
      .uiModules(List.of(new Dependency("folio_test", "2.0.0", null)));

    var beModule = new ModuleDefinition().id("mod-test-1.0.0").name("mod-test").version("1.0.0")
      .url("http://registry/mod-test");
    var uiModule = new ModuleDefinition().id("folio_test-2.0.0").name("folio_test").version("2.0.0")
      .url("http://registry/folio_test");

    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(List.of(beModule), List.of(Map.of("id", "mod-test-1.0.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(List.of(uiModule), List.of(Map.of("id", "folio_test-2.0.0"))));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(false);

    var result = service.create(template);

    assertThat(result.getModuleDescriptors()).isEqualTo(List.of(Map.of("id", "mod-test-1.0.0")));
    assertThat(result.getUiModuleDescriptors()).isEqualTo(List.of(Map.of("id", "folio_test-2.0.0")));
    assertThat(result.getModules().get(0).getUrl()).isNull();
    assertThat(result.getUiModules().get(0).getUrl()).isNull();
  }

  @Test
  void create_negative_invalidApplicationId() {
    var template = new ApplicationDescriptorTemplate()
      .id("wrong-id")
      .name("test-app")
      .version("1.0.0");

    assertThrows(MojoExecutionException.class, () -> service.create(template));
  }

  @Test
  @SneakyThrows
  void create_positive_validApplicationId() {
    var template = new ApplicationDescriptorTemplate()
      .id("test-app-1.0.0")
      .name("test-app")
      .version("1.0.0")
      .modules(List.of());

    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);

    var result = service.create(template);

    assertThat(result.getId()).isEqualTo("test-app-1.0.0");
  }

  @Test
  @SneakyThrows
  void create_positive_nullModulesAndUiModules() {
    var template = new ApplicationDescriptorTemplate()
      .name("test-app")
      .version("1.0.0");

    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);

    var result = service.create(template);

    assertThat(result.getModules()).isEmpty();
    assertThat(result.getUiModules()).isEmpty();
  }

  @Test
  @SneakyThrows
  void create_positive_defaultDescriptionFromMavenProject() {
    var template = new ApplicationDescriptorTemplate()
      .name("test-app")
      .version("1.0.0")
      .modules(List.of());

    when(mavenProject.getDescription()).thenReturn("Default description from pom.xml");
    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);

    var result = service.create(template);

    assertThat(result.getDescription()).isEqualTo("Default description from pom.xml");
  }

  @Test
  @SneakyThrows
  void create_positive_templateDescriptionOverridesMavenDescription() {
    var template = new ApplicationDescriptorTemplate()
      .name("test-app")
      .version("1.0.0")
      .modules(List.of());
    template.setDescription("Template description");

    when(mavenProject.getDescription()).thenReturn("Maven description");
    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);

    var result = service.create(template);

    assertThat(result.getDescription()).isEqualTo("Template description");
  }

  @Test
  @SneakyThrows
  void create_positive_nullDependenciesConvertedToEmptyList() {
    var template = new ApplicationDescriptorTemplate()
      .name("test-app")
      .version("1.0.0")
      .modules(List.of());

    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);

    var result = service.create(template);

    assertThat(result.getDependencies()).isEmpty();
  }

  @Test
  void create_negative_templateWithNameNullAndVersionNotNull() {
    var template = new ApplicationDescriptorTemplate()
      .version("1.0.0")
      .modules(List.of());

    assertThrows(NullPointerException.class, () -> service.create(template));
  }
}
