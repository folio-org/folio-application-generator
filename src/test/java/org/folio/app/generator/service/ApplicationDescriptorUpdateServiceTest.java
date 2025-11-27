package org.folio.app.generator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.app.generator.model.types.ModuleType.BE;
import static org.folio.app.generator.model.types.ModuleType.UI;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.ModulesLoadResult;
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

  @Mock private PluginConfig pluginConfig;
  @Mock private ModuleDescriptorService moduleDescriptorService;
  @Mock private ModuleVersionService moduleVersionService;
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
  void update_negative_invalidModuleVersion() {
    var application = new ApplicationDescriptor()
      .modules(List.of(
        new ModuleDefinition().name("module1").version("1.0.0"),
        new ModuleDefinition().name("module2").version("2.0.0"),
        new ModuleDefinition().name("module3").version("latest")))
      .uiModules(List.of(
        new ModuleDefinition().name("module-u1").version("1.0.0"),
        new ModuleDefinition().name("module-u2").version("2.0.10010000000000158"),
        new ModuleDefinition().name("module-u3").version("latest")));

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "module1-1.1.0,module2-2.0.0,module3:latest",
        "module-u1-1.1.0,module-u2-2.0.10010000000000158,module-u3:latest"));
  }

  @Test
  @SneakyThrows
  void update_negative_downgradeModule() {
    var application = new ApplicationDescriptor()
      .modules(List.of(new ModuleDefinition().name("module1").version("2.0.0")))
      .uiModules(List.of(new ModuleDefinition().name("module-u1").version("2.0.0")));

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "module1-1.0.0", "module-u1-1.0.0"));
  }

  @Test
  @SneakyThrows
  void update_negative_moduleNotInDescriptor() {
    var application = new ApplicationDescriptor()
      .modules(List.of(
        new ModuleDefinition().name("module1").version("1.0.0"),
        new ModuleDefinition().name("module2").version("2.0.0")))
      .uiModules(List.of(
        new ModuleDefinition().name("module-u1").version("1.0.0"),
        new ModuleDefinition().name("module-u2").version("2.0.0")));

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "module3-1.0.0", "module-u3-1.0.0"));
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

    var result = updateService.update(application,
      "module1-1.1.0,module2:latest",
      "uiModule1-1.0.1,uiModule2-1.0.10010000000200"
    );

    assertThat(descriptorsCaptor.getValue()).isEqualTo(
      List.of(
        new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0"),
        new ModuleDefinition().id("module2-latest").name("module2").version("latest")));
    assertThat(uiDescriptorsCaptor.getValue()).isEqualTo(
      List.of(
        new ModuleDefinition().id("uiModule1-1.0.1").name("uiModule1").version("1.0.1"),
        new ModuleDefinition().id("uiModule2-1.0.10010000000200").name("uiModule2").version("1.0.10010000000200")));

    assertThat(result.getId()).isEqualTo("name-1.0.1-SNAPSHOT");
    assertThat(result.getVersion()).isEqualTo("1.0.1-SNAPSHOT");

    assertThat(result.getModules()).isEqualTo(
      List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0"),
        new ModuleDefinition().id("module2-latest").name("module2").version("latest"),
        new ModuleDefinition().id("module3-1.1.0").name("module3").version("1.1.0")));
    assertThat(result.getUiModules()).isEqualTo(
      List.of(new ModuleDefinition().id("uiModule1-1.0.1").name("uiModule1").version("1.0.1"),
        new ModuleDefinition().id("uiModule2-1.0.10010000000200").name("uiModule2").version("1.0.10010000000200")));

    assertThat(result.getModuleDescriptors()).isEqualTo(List.of(
      Map.of("id", "module1-1.1.0"),
      Map.of("id", "module2:latest"),
      Map.of("id", "module3-1.1.0")));
    assertThat(result.getUiModuleDescriptors()).isEqualTo(List.of(
      Map.of("id", "uiModule1-1.0.1"),
      Map.of("id", "uiModule2-1.0.10010000000200")));

    verify(moduleDescriptorService).loadModules(eq(BE), anyList());
    verify(moduleDescriptorService).loadModules(eq(UI), anyList());
  }

  @Test
  @SneakyThrows
  void update_negative_nullDescriptors() {
    var application = new ApplicationDescriptor()
      .moduleDescriptors(null)
      .uiModuleDescriptors(null);

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "module1-1.1.0", "uiModule1-1.0.1"));
  }

  @Test
  @SneakyThrows
  void update_negative_invalidVersionFormat() {
    var application = new ApplicationDescriptor()
      .modules(List.of(new ModuleDefinition().name("module1").version("1.x.0")))
      .uiModules(List.of(new ModuleDefinition().name("uiModule1").version("1.0.0")));

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "module1-2.0.0", "uiModule1-1.0.1"));
  }

  @Test
  void update_negative_invalidModuleIdFormat() {
    var application = new ApplicationDescriptor();

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "invalid-module-format", ""));
  }

  @Test
  @SneakyThrows
  void update_negative_invalidNewSemverVersion() {
    var application = new ApplicationDescriptor()
      .modules(List.of(new ModuleDefinition().name("module1").version("1.0.0")))
      .uiModules(List.of(new ModuleDefinition().name("uiModule1").version("1.0.0")));

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "module1-1.x.0", ""));
  }

  @Test
  @SneakyThrows
  void update_negative_mixedValidAndInvalidModules() {
    var application = new ApplicationDescriptor()
      .modules(List.of(
        new ModuleDefinition().name("module1").version("1.0.0"),
        new ModuleDefinition().name("module2").version("2.0.0")))
      .uiModules(List.of(new ModuleDefinition().name("uiModule1").version("1.0.0")));

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "module1-2.0.0,module2-1.0.0", "uiModule1-1.0.0"));
  }

  @Test
  void update_negative_blankModulesInput() {
    var application = new ApplicationDescriptor();

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "", ""));
  }

  @Test
  @SneakyThrows
  void update_positive_withBuildNumber_preRelease() {
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

    var result = updateService.update(application, "", "uiModule1-1.1.0");

    assertThat(result.getVersion()).isEqualTo("1.0.0-SNAPSHOT.125");
    assertThat(result.getId()).isEqualTo("name-1.0.0-SNAPSHOT.125");
  }

  @Test
  @SneakyThrows
  void update_positive_withBuildNumber_preReleaseNoBuildInVersion() {
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

    var result = updateService.update(application, "module1-1.1.0", "");

    assertThat(result.getVersion()).isEqualTo("1.0.0-SNAPSHOT.125");
  }

  @Test
  @SneakyThrows
  void update_positive_withBuildNumber_releaseVersion() {
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

    var result = updateService.update(application, "module1-1.1.0", "");

    assertThat(result.getVersion()).isEqualTo("1.0.1");
  }

  @Test
  @SneakyThrows
  void update_positive_invalidVersionFormat() {
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

    var result = updateService.update(application, "module1-1.1.0", "");

    assertThat(result.getVersion()).isEqualTo("invalid");
  }

  @Test
  @SneakyThrows
  void update_positive_nullModuleDescriptors() {
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

    var result = updateService.update(application, "module1-1.1.0", "");

    assertThat(result.getModuleDescriptors()).isNull();
    assertThat(result.getUiModuleDescriptors()).isNull();
  }
}

