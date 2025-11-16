package org.folio.app.generator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.app.generator.model.types.ModuleType.BE;
import static org.folio.app.generator.model.types.ModuleType.UI;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
import org.folio.app.generator.support.UnitTest;
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
  @Mock private JsonProvider jsonProvider;
  @Mock private ModuleDescriptorService moduleDescriptorService;
  @Mock private ModuleVersionService moduleVersionService;
  @Captor private ArgumentCaptor<ApplicationDescriptor> applicationCaptor;
  @Captor private ArgumentCaptor<List<ModuleDefinition>> descriptorsCaptor;
  @Captor private ArgumentCaptor<List<ModuleDefinition>> uiDescriptorsCaptor;
  @InjectMocks private ApplicationDescriptorUpdateService updateService;

  @BeforeEach
  void setUp() throws Exception {
    when(moduleVersionService.resolveModulesConstraints(anyList(), eq(BE)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(moduleVersionService.resolveModulesConstraints(anyList(), eq(UI)))
      .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @SneakyThrows
  void update_negative_invalidModuleVersion() {
    List<Map<String, Object>> modules = List.of(
      Map.of("id", "module1-1.0.0"),
      Map.of("id", "module2-2.0.0"),
      Map.of("id", "module3:latest"));
    var application = new ApplicationDescriptor().moduleDescriptors(modules);

    assertThrows(IllegalArgumentException.class,
      () -> updateService.update(application, "module1-1.1.0,module2-2.0.0,module3:latest", null));
  }

  @Test
  @SneakyThrows
  void update_positive() {
    List<Map<String, Object>> modules = List.of(Map.of("id", "module1-1.0.0"), Map.of("id", "module2-2.0.0"),
      Map.of("id", "module3-1.1.0"));
    List<Map<String, Object>> uiModules = List.of(Map.of("id", "uiModule1-1.0.0"));
    var build = new Build();
    build.setDirectory("dir");
    var uiModuleDefinitions = List.of(new ModuleDefinition().id("uiModule1-1.0.1").name("uiModule1").version("1.0.1"));

    final var application = new ApplicationDescriptor()
      .id("name-1.0.0-SNAPSHOT")
      .name("name")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(new ModuleDefinition().id("module1-1.0.0").name("module1").version("1.0.0"),
        new ModuleDefinition().id("module2-2.0.0").name("module2").version("2.0.0"),
        new ModuleDefinition().id("module3-1.1.0").name("module3").version("1.1.0")))
      .uiModules(uiModuleDefinitions)
      .moduleDescriptors(modules).uiModuleDescriptors(uiModules);

    when(moduleDescriptorService.loadModules(eq(BE), descriptorsCaptor.capture())).thenReturn(
      new ModulesLoadResult(List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0"),
        new ModuleDefinition().id("module2-latest").name("module2").version("latest")),
        List.of(Map.of("id", "module1-1.1.0"), Map.of("id", "module2:latest"))));
    when(moduleDescriptorService.loadModules(eq(UI), uiDescriptorsCaptor.capture()))
      .thenReturn(
        new ModulesLoadResult(List.of(new ModuleDefinition().id("uiModule1-1.0.1").name("uiModule1").version("1.0.1")),
          List.of(Map.of("id", "uiModule1-1.0.1"))));

    when(mavenProject.getBuild()).thenReturn(build);
    doNothing().when(jsonProvider).writeApplication(applicationCaptor.capture(), any());

    updateService.update(application, "module1-1.1.0,module2:latest", "uiModule1-1.0.1");

    assertThat(descriptorsCaptor.getValue()).isEqualTo(
      List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0"),
        new ModuleDefinition().id("module2-latest").name("module2").version("latest")));
    assertThat(uiDescriptorsCaptor.getValue()).isEqualTo(
      List.of(new ModuleDefinition().id("uiModule1-1.0.1").name("uiModule1").version("1.0.1")));

    assertThat(applicationCaptor.getValue().getId()).isEqualTo("name-1.0.1-SNAPSHOT");
    assertThat(applicationCaptor.getValue().getVersion()).isEqualTo("1.0.1-SNAPSHOT");

    assertThat(applicationCaptor.getValue().getModules()).isEqualTo(
      List.of(new ModuleDefinition().id("module1-1.1.0").name("module1").version("1.1.0"),
        new ModuleDefinition().id("module2-latest").name("module2").version("latest"),
        new ModuleDefinition().id("module3-1.1.0").name("module3").version("1.1.0")));
    assertThat(applicationCaptor.getValue().getUiModules()).isEqualTo(uiModuleDefinitions);

    assertThat(applicationCaptor.getValue().getModuleDescriptors()).isEqualTo(
      List.of(Map.of("id", "module1-1.1.0"), Map.of("id", "module2:latest"), Map.of("id", "module3-1.1.0")));
    assertThat(applicationCaptor.getValue().getUiModuleDescriptors())
      .isEqualTo(List.of(Map.of("id", "uiModule1-1.0.1")));

    verify(moduleDescriptorService).loadModules(eq(BE), anyList());
    verify(moduleDescriptorService).loadModules(eq(UI), anyList());
    verify(jsonProvider).writeApplication(eq(application), any(String.class));
  }
}
