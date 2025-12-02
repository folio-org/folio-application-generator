package org.folio.app.generator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.app.generator.model.types.ModuleType.BE;
import static org.folio.app.generator.model.types.ModuleType.UI;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.ModulesLoadResult;
import org.folio.app.generator.model.UpdateConfig;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.PluginConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ApplicationDescriptorUpdateServiceTest {

  @Mock private Log log;
  @Mock private MavenProject mavenProject;
  @Mock private PluginConfig pluginConfig;
  @Mock private JsonProvider jsonProvider;
  @Mock private ModuleDescriptorService moduleDescriptorService;
  @Mock private ModuleVersionService moduleVersionService;
  @Captor private ArgumentCaptor<ApplicationDescriptor> applicationCaptor;
  @Captor private ArgumentCaptor<List<ModuleDefinition>> descriptorsCaptor;
  @Captor private ArgumentCaptor<List<ModuleDefinition>> uiDescriptorsCaptor;
  @InjectMocks private ApplicationDescriptorUpdateService updateService;

  @BeforeEach
  void setUp() throws Exception {
    lenient().when(moduleVersionService.resolveModulesConstraints(anyList(), eq(BE)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(moduleVersionService.resolveModulesConstraints(anyList(), eq(UI)))
      .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @SneakyThrows
  void update_negative_downgradeModuleNotAllowed() {
    var application = new ApplicationDescriptor()
      .modules(List.of(new ModuleDefinition().name("module1").version("2.0.0")))
      .uiModules(List.of(new ModuleDefinition().name("module-u1").version("2.0.0")));
    var config = UpdateConfig.defaults();

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "module1-1.0.0", "module-u1-1.0.0", config));
  }

  @Test
  @SneakyThrows
  void update_negative_moduleNotInDescriptorNotAllowed() {
    var application = new ApplicationDescriptor()
      .modules(List.of(
        new ModuleDefinition().name("module1").version("1.0.0"),
        new ModuleDefinition().name("module2").version("2.0.0")))
      .uiModules(List.of(
        new ModuleDefinition().name("module-u1").version("1.0.0"),
        new ModuleDefinition().name("module-u2").version("2.0.0")));
    var config = UpdateConfig.defaults();

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "module3-1.0.0", "module-u3-1.0.0", config));
  }

  @Test
  @SneakyThrows
  void update_positive() {
    List<Map<String, Object>> modules = List.of(
      Map.of("id", "module1-1.0.0"),
      Map.of("id", "module2-2.0.0"),
      Map.of("id", "module3-1.1.0"));
    List<Map<String, Object>> uiModules = List.of(
      Map.of("id", "uiModule1-1.0.0"),
      Map.of("id", "uiModule2-1.0.10010000000158"));
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(
        new ModuleDefinition().id("module1-1.0.0").name("module1").version("1.0.0"),
        new ModuleDefinition().id("module2-2.0.0").name("module2").version("2.0.0"),
        new ModuleDefinition().id("module3-1.1.0").name("module3").version("1.1.0")))
      .uiModules(List.of(
        new ModuleDefinition().id("uiModule1-1.0.0").name("uiModule1").version("1.0.0"),
        new ModuleDefinition().id("uiModule2-1.0.10010000000158").name("uiModule2").version("1.0.10010000000158")))
      .moduleDescriptors(modules)
      .uiModuleDescriptors(uiModules);

    when(moduleDescriptorService.loadModules(eq(BE), descriptorsCaptor.capture())).thenReturn(
      new ModulesLoadResult(
        List.of(
          new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0"),
          new ModuleDefinition().id("module2-latest").name("module2").version("latest")),
        List.of(
          Map.of("id", "module1-1.1.0"),
          Map.of("id", "module2:latest"))));
    when(moduleDescriptorService.loadModules(eq(UI), uiDescriptorsCaptor.capture()))
      .thenReturn(
        new ModulesLoadResult(
          List.of(
            new ModuleDefinition().id("uiModule1-1.0.1").name("uiModule1").version("1.0.1"),
            new ModuleDefinition().id("uiModule2-1.0.10010000000200").name("uiModule2").version("1.0.10010000000200")),
          List.of(
            Map.of("id", "uiModule1-1.0.1"),
            Map.of("id", "uiModule2-1.0.10010000000200"))));

    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application,
      "module1-1.1.0,module2:latest",
      "uiModule1-1.0.1,uiModule2-1.0.10010000000200",
      UpdateConfig.defaults()
    );

    assertThat(descriptorsCaptor.getValue()).isEqualTo(
      List.of(
        new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0"),
        new ModuleDefinition().id("module2-latest").name("module2").version("latest")));
    assertThat(uiDescriptorsCaptor.getValue()).isEqualTo(
      List.of(
        new ModuleDefinition().id("uiModule1-1.0.1").name("uiModule1").version("1.0.1"),
        new ModuleDefinition().id("uiModule2-1.0.10010000000200").name("uiModule2").version("1.0.10010000000200")));

    assertThat(applicationCaptor.getValue().getId()).isEqualTo("name-1.0.1-SNAPSHOT");
    assertThat(applicationCaptor.getValue().getVersion()).isEqualTo("1.0.1-SNAPSHOT");

    assertThat(applicationCaptor.getValue().getModules()).isEqualTo(
      List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0"),
        new ModuleDefinition().id("module2-latest").name("module2").version("latest"),
        new ModuleDefinition().id("module3-1.1.0").name("module3").version("1.1.0")));
    assertThat(applicationCaptor.getValue().getUiModules()).isEqualTo(
      List.of(new ModuleDefinition().id("uiModule1-1.0.1").name("uiModule1").version("1.0.1"),
        new ModuleDefinition().id("uiModule2-1.0.10010000000200").name("uiModule2").version("1.0.10010000000200")));

    assertThat(applicationCaptor.getValue().getModuleDescriptors()).isEqualTo(List.of(
      Map.of("id", "module1-1.1.0"),
      Map.of("id", "module2:latest"),
      Map.of("id", "module3-1.1.0")));
    assertThat(applicationCaptor.getValue().getUiModuleDescriptors()).isEqualTo(List.of(
      Map.of("id", "uiModule1-1.0.1"),
      Map.of("id", "uiModule2-1.0.10010000000200")));

    verify(moduleDescriptorService).loadModules(eq(BE), anyList());
    verify(moduleDescriptorService).loadModules(eq(UI), anyList());
    verify(jsonProvider).writeApplication(eq(application), any(String.class));
  }

  @Test
  void update_negative_invalidModuleIdFormat() {
    var application = new ApplicationDescriptor();
    var config = UpdateConfig.defaults();

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "invalid-module-format", "", config));
  }

  @Test
  @SneakyThrows
  void update_positive_noChanges_skipsUpdate() {
    var application = new ApplicationDescriptor()
      .modules(List.of(new ModuleDefinition().name("module1").version("1.0.0")))
      .uiModules(List.of());

    updateService.update(application, "", "", UpdateConfig.defaults());

    verify(jsonProvider, never()).writeApplication(any(), any());
  }

  @Test
  @SneakyThrows
  void update_positive_withBuildNumber_preRelease() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT.124")
      .name("name")
      .version("1.0.0-SNAPSHOT.124")
      .modules(List.of(new ModuleDefinition().name("module1").version("1.0.0")))
      .uiModules(List.of(new ModuleDefinition().name("uiModule1").version("1.0.0")));

    when(pluginConfig.getBuildNumber()).thenReturn("125");
    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(
          List.of(new ModuleDefinition().id("uiModule1-1.1.0").name("uiModule1").version("1.1.0")),
          List.of(Map.of("id", "uiModule1-1.1.0"))));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "", "uiModule1-1.1.0", UpdateConfig.defaults());

    assertThat(applicationCaptor.getValue().getVersion()).isEqualTo("1.0.0-SNAPSHOT.125");
    assertThat(applicationCaptor.getValue().getId()).isEqualTo("name-1.0.0-SNAPSHOT.125");
  }

  @Test
  @SneakyThrows
  void update_positive_withBuildNumber_preReleaseNoBuildInVersion() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(new ModuleDefinition().name("module1").version("1.0.0")))
      .uiModules(List.of(new ModuleDefinition().name("uiModule1").version("1.0.0")));

    when(pluginConfig.getBuildNumber()).thenReturn("125");
    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0")),
        List.of(Map.of("id", "module1-1.1.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "module1-1.1.0", "", UpdateConfig.defaults());

    assertThat(applicationCaptor.getValue().getVersion()).isEqualTo("1.0.0-SNAPSHOT.125");
  }

  @Test
  @SneakyThrows
  void update_positive_withBuildNumber_releaseVersion() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0")
      .name("name")
      .version("1.0.0")
      .modules(List.of(new ModuleDefinition().name("module1").version("1.0.0")))
      .uiModules(List.of(new ModuleDefinition().name("uiModule1").version("1.0.0")));

    when(pluginConfig.getBuildNumber()).thenReturn("125");
    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0")),
        List.of(Map.of("id", "module1-1.1.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "module1-1.1.0", "", UpdateConfig.defaults());

    assertThat(applicationCaptor.getValue().getVersion()).isEqualTo("1.0.1");
  }

  @Test
  @SneakyThrows
  void update_positive_invalidVersionFormat() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-invalid")
      .name("name")
      .version("invalid")
      .modules(List.of(new ModuleDefinition().name("module1").version("1.0.0")))
      .uiModules(List.of(new ModuleDefinition().name("uiModule1").version("1.0.0")));

    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0")),
        List.of(Map.of("id", "module1-1.1.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "module1-1.1.0", "", UpdateConfig.defaults());

    assertThat(applicationCaptor.getValue().getVersion()).isEqualTo("invalid");
  }

  @Test
  @SneakyThrows
  void update_positive_nullModuleDescriptors() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(new ModuleDefinition().name("module1").version("1.0.0")))
      .uiModules(List.of(new ModuleDefinition().name("uiModule1").version("1.0.0")))
      .moduleDescriptors(null)
      .uiModuleDescriptors(null);

    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0")),
        List.of(Map.of("id", "module1-1.1.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "module1-1.1.0", "", UpdateConfig.defaults());

    assertThat(applicationCaptor.getValue().getModuleDescriptors())
      .isEqualTo(List.of(Map.of("id", "module1-1.1.0")));
  }

  @Test
  @SneakyThrows
  void update_positive_allowDowngrade() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(new ModuleDefinition().name("module1").version("2.0.0")))
      .uiModules(List.of());

    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module1-1.0.0").name("module1").version("1.0.0")),
        List.of(Map.of("id", "module1-1.0.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    var config = UpdateConfig.builder().allowDowngrade(true).build();
    updateService.update(application, "module1-1.0.0", "", config);

    assertThat(applicationCaptor.getValue().getModules().get(0).getVersion()).isEqualTo("1.0.0");
  }

  @Test
  @SneakyThrows
  void update_positive_allowAddModules() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(new ModuleDefinition().id("module1-1.0.0").name("module1").version("1.0.0")))
      .uiModules(List.of());

    // Only module2 is changed (new), module1 is unchanged (same version)
    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module2-1.0.0").name("module2").version("1.0.0")),
        List.of(Map.of("id", "module2-1.0.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    var config = UpdateConfig.builder().allowAddModules(true).build();
    updateService.update(application, "module1-1.0.0,module2-1.0.0", "", config);

    assertThat(applicationCaptor.getValue().getModules()).hasSize(2);
  }

  @Test
  @SneakyThrows
  void update_positive_removeUnlistedModules() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(
        new ModuleDefinition().id("module1-1.0.0").name("module1").version("1.0.0"),
        new ModuleDefinition().id("module2-1.0.0").name("module2").version("1.0.0")))
      .uiModules(List.of())
      .moduleDescriptors(List.of(Map.of("id", "module1-1.0.0"), Map.of("id", "module2-1.0.0")));

    // module1 is unchanged (same version), module2 is removed - no changed modules to load
    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    var config = UpdateConfig.builder().removeUnlistedModules(true).build();
    updateService.update(application, "module1-1.0.0", "", config);

    assertThat(applicationCaptor.getValue().getModules()).hasSize(1);
    assertThat(applicationCaptor.getValue().getModules().get(0).getName()).isEqualTo("module1");
  }

  @Test
  @SneakyThrows
  void update_positive_moduleUrlsOnlyMode() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(new ModuleDefinition().name("module1").version("1.0.0")))
      .uiModules(List.of())
      .moduleDescriptors(List.of(Map.of("id", "module1-1.0.0")));

    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);
    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0")
          .url("http://registry/module1")),
        List.of(Map.of("id", "module1-1.1.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "module1-1.1.0", "", UpdateConfig.defaults());

    assertThat(applicationCaptor.getValue().getModules().get(0).getUrl()).isEqualTo("http://registry/module1");
    assertThat(applicationCaptor.getValue().getModuleDescriptors()).isEmpty();
  }

  @Test
  @SneakyThrows
  void update_positive_fullDescriptorMode() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(new ModuleDefinition().name("module1").version("1.0.0")))
      .uiModules(List.of())
      .moduleDescriptors(List.of(Map.of("id", "module1-1.0.0")));

    when(pluginConfig.isModuleUrlsOnly()).thenReturn(false);
    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0")
          .url("http://registry/module1")),
        List.of(Map.of("id", "module1-1.1.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "module1-1.1.0", "", UpdateConfig.defaults());

    assertThat(applicationCaptor.getValue().getModules().get(0).getUrl()).isNull();
    assertThat(applicationCaptor.getValue().getModuleDescriptors()).isNotEmpty();
  }

  @Test
  @SneakyThrows
  void update_positive_invalidExistingSemver_treatedAsUnchanged() {
    var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(new ModuleDefinition().name("module1").version("^1.0.0")))
      .uiModules(List.of());

    updateService.update(application, "module1-1.5.0", "", UpdateConfig.defaults());

    verify(jsonProvider, never()).writeApplication(any(), any());
  }

  @Test
  @SneakyThrows
  void update_positive_invalidUpdateSemver_withValidModuleChanges() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(
        new ModuleDefinition().name("module1").version("1.0.0"),
        new ModuleDefinition().name("module2").version("1.0.0")))
      .uiModules(List.of());

    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module2-1.1.0").name("module2").version("1.1.0")),
        List.of(Map.of("id", "module2-1.1.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "module1-1x,module2-1.1.0", "", UpdateConfig.defaults());

    assertThat(applicationCaptor.getValue().getModules()).hasSize(2);
    assertThat(applicationCaptor.getValue().getModules().get(0).getVersion()).isEqualTo("1.1.0");
    assertThat(applicationCaptor.getValue().getModules().get(1).getVersion()).isEqualTo("1.0.0");
    verify(log).warn("Unable to compare versions '1.0.0' and '1x' - treating as unchanged");
  }

  @Test
  @SneakyThrows
  void update_positive_descriptorWithInvalidModuleId_skipped() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(new ModuleDefinition().id("module1-1.0.0").name("module1").version("1.0.0")))
      .uiModules(List.of())
      .moduleDescriptors(List.of(
        Map.of("id", "module1-1.0.0"),
        Map.of("id", "invalid")));

    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0")),
        List.of(Map.of("id", "module1-1.1.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "module1-1.1.0", "", UpdateConfig.defaults());

    assertThat(applicationCaptor.getValue().getModuleDescriptors()).hasSize(1);
    assertThat(applicationCaptor.getValue().getModuleDescriptors().get(0)).containsEntry("id", "module1-1.1.0");
  }

  @Test
  @SneakyThrows
  void update_positive_moduleNotInUpdateList_descriptorPreserved() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(
        new ModuleDefinition().id("module1-1.0.0").name("module1").version("1.0.0"),
        new ModuleDefinition().id("module2-1.0.0").name("module2").version("1.0.0")))
      .uiModules(List.of())
      .moduleDescriptors(List.of(
        Map.of("id", "module1-1.0.0"),
        Map.of("id", "module2-1.0.0")));

    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0")),
        List.of(Map.of("id", "module1-1.1.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    var config = UpdateConfig.builder().removeUnlistedModules(false).build();
    updateService.update(application, "module1-1.1.0", "", config);

    assertThat(applicationCaptor.getValue().getModuleDescriptors()).hasSize(2);
    assertThat(applicationCaptor.getValue().getModuleDescriptors().get(0)).containsEntry("id", "module1-1.1.0");
    assertThat(applicationCaptor.getValue().getModuleDescriptors().get(1)).containsEntry("id", "module2-1.0.0");
  }

  @Test
  @SneakyThrows
  void update_positive_moduleUrlsOnlyMode_invalidDescriptorIdFiltered() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(
        new ModuleDefinition().id("module1-1.0.0").name("module1").version("1.0.0"),
        new ModuleDefinition().id("module2-1.0.0").name("module2").version("1.0.0")))
      .uiModules(List.of())
      .moduleDescriptors(List.of(
        Map.of("id", "module1-1.0.0"),
        Map.of("id", "invalid"),
        Map.of("id", "module2-1.0.0")));

    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);
    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0")
          .url("http://registry/module1")),
        List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "module1-1.1.0,module2-1.0.0", "", UpdateConfig.defaults());

    assertThat(applicationCaptor.getValue().getModuleDescriptors()).hasSize(1);
    assertThat(applicationCaptor.getValue().getModuleDescriptors().get(0)).containsEntry("id", "module2-1.0.0");
    verify(log).warn("Skipping descriptor with invalid module ID: invalid");
  }

  @Test
  @SneakyThrows
  void update_positive_moduleInUpdateButNoNewDescriptor_existingPreserved() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(
        new ModuleDefinition().id("module1-1.0.0").name("module1").version("1.0.0"),
        new ModuleDefinition().id("module2-1.0.0").name("module2").version("1.0.0")))
      .uiModules(List.of())
      .moduleDescriptors(List.of(
        Map.of("id", "module1-1.0.0"),
        Map.of("id", "module2-1.0.0")));

    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(
          new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0"),
          new ModuleDefinition().id("module2-1.0.0").name("module2").version("1.0.0")),
        List.of(Map.of("id", "module1-1.1.0"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "module1-1.1.0,module2-1.0.0", "", UpdateConfig.defaults());

    assertThat(applicationCaptor.getValue().getModuleDescriptors()).hasSize(2);
    assertThat(applicationCaptor.getValue().getModuleDescriptors().get(0)).containsEntry("id", "module1-1.1.0");
    assertThat(applicationCaptor.getValue().getModuleDescriptors().get(1)).containsEntry("id", "module2-1.0.0");
  }

  @Test
  @SneakyThrows
  void update_positive_moduleUrlsOnlyMode_descriptorVersionMismatch() {
    var build = new Build();
    build.setDirectory("dir");

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(
        new ModuleDefinition().id("module1-1.0.0").name("module1").version("1.0.0"),
        new ModuleDefinition().id("module2-1.0.0").name("module2").version("1.0.0")))
      .uiModules(List.of())
      .moduleDescriptors(List.of(
        Map.of("id", "module1-1.0.0"),
        Map.of("id", "module2-2.0.0")));

    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);
    when(moduleDescriptorService.loadModules(eq(BE), anyList()))
      .thenReturn(new ModulesLoadResult(
        List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0")
          .url("http://registry/module1")),
        List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList()))
      .thenReturn(new ModulesLoadResult(List.of(), List.of()));
    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "module1-1.1.0,module2-1.0.0", "", UpdateConfig.defaults());

    assertThat(applicationCaptor.getValue().getModuleDescriptors()).isEmpty();
    verify(log).warn("Descriptor version mismatch for 'module2': expected 1.0.0, found 2.0.0");
  }
}
