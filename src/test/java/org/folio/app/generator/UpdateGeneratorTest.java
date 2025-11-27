package org.folio.app.generator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.ResolvedApplicationDescriptor;
import org.folio.app.generator.model.types.ModuleUrlsMode;
import org.folio.app.generator.service.ApplicationDescriptorUpdateService;
import org.folio.app.generator.service.JsonProvider;
import org.folio.app.generator.service.ModuleRegistryProvider;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.PluginConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.GenericApplicationContext;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UpdateGeneratorTest {

  private static final String OUTPUT_DIR = "/target";
  private static final String APP_ID = "test-app-1.0.0";

  @Mock private ModuleRegistryProvider mockRegistryProvider;
  @Mock private ApplicationContextBuilder mockContextBuilder;
  @Mock private GenericApplicationContext mockGenericApplicationContext;
  @Mock private ApplicationDescriptorUpdateService mockUpdateService;
  @Mock private JsonProvider mockJsonProvider;
  @Mock private PluginConfig mockPluginConfig;
  @Mock private MavenProject mockMavenProject;
  @Mock private Build mockBuild;
  @InjectMocks private UpdateGenerator mojo;

  private ApplicationDescriptor inputDescriptor;
  private ResolvedApplicationDescriptor resolved;

  @BeforeEach
  void setUp() {
    inputDescriptor = new ApplicationDescriptor()
      .id(APP_ID)
      .name("test-app")
      .version("1.0.0");

    resolved = ResolvedApplicationDescriptor.builder()
      .id(APP_ID)
      .name("test-app")
      .version("1.0.0")
      .modules(List.of(
        new ModuleDefinition().id("mod-test-1.0.0").name("mod-test").version("1.0.0")
          .url("http://registry/mod-test")))
      .uiModules(List.of(
        new ModuleDefinition().id("folio_test-2.0.0").name("folio_test").version("2.0.0")
          .url("http://registry/folio_test")))
      .moduleDescriptors(List.of(Map.of("id", "mod-test-1.0.0")))
      .uiModuleDescriptors(List.of(Map.of("id", "folio_test-2.0.0")))
      .dependencies(List.of(new Dependency("app-platform", "^1.0.0", null)))
      .build();
  }

  @Test
  @SneakyThrows
  void execute_positive_modeFalse_writesFullDescriptorOnly() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.cmdModulesString = "mod-test:1.0.0";
    mojo.cmdUiModulesString = "folio_test:2.0.0";
    mojo.mavenProject = mockMavenProject;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);
    when(mockUpdateService.update(inputDescriptor, "mod-test:1.0.0", "folio_test:2.0.0"))
      .thenReturn(resolved);
    when(mockPluginConfig.getModuleUrlsMode()).thenReturn(ModuleUrlsMode.FALSE);
    when(mockMavenProject.getBuild()).thenReturn(mockBuild);
    when(mockBuild.getDirectory()).thenReturn(OUTPUT_DIR);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockJsonProvider).writeApplication(any(ApplicationDescriptor.class), eq(OUTPUT_DIR));
    verify(mockJsonProvider, never()).writeApplication(any(ApplicationDescriptor.class), any(), any());
  }

  @Test
  @SneakyThrows
  void execute_positive_modeTrue_writesUrlOnlyDescriptor() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.cmdModulesString = "mod-test:1.0.0";
    mojo.cmdUiModulesString = "folio_test:2.0.0";
    mojo.mavenProject = mockMavenProject;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);
    when(mockUpdateService.update(inputDescriptor, "mod-test:1.0.0", "folio_test:2.0.0"))
      .thenReturn(resolved);
    when(mockPluginConfig.getModuleUrlsMode()).thenReturn(ModuleUrlsMode.TRUE);
    when(mockMavenProject.getBuild()).thenReturn(mockBuild);
    when(mockBuild.getDirectory()).thenReturn(OUTPUT_DIR);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockJsonProvider, never()).writeApplication(any(ApplicationDescriptor.class), eq(OUTPUT_DIR));
    verify(mockJsonProvider).writeApplication(any(ApplicationDescriptor.class), eq(OUTPUT_DIR), eq(APP_ID + ".json"));
  }

  @Test
  @SneakyThrows
  void execute_positive_modeBoth_writesBothDescriptors() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.cmdModulesString = "mod-test:1.0.0";
    mojo.cmdUiModulesString = "folio_test:2.0.0";
    mojo.mavenProject = mockMavenProject;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);
    when(mockUpdateService.update(inputDescriptor, "mod-test:1.0.0", "folio_test:2.0.0"))
      .thenReturn(resolved);
    when(mockPluginConfig.getModuleUrlsMode()).thenReturn(ModuleUrlsMode.BOTH);
    when(mockMavenProject.getBuild()).thenReturn(mockBuild);
    when(mockBuild.getDirectory()).thenReturn(OUTPUT_DIR);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockJsonProvider).writeApplication(any(ApplicationDescriptor.class), eq(OUTPUT_DIR));
    verify(mockJsonProvider).writeApplication(any(ApplicationDescriptor.class), eq(OUTPUT_DIR),
      eq(APP_ID + ".url.json"));
  }

  @Test
  @SneakyThrows
  void execute_positive_usesDefaultDescriptorPath() {
    mojo.mavenProject = mockMavenProject;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile(null, ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);
    when(mockUpdateService.update(inputDescriptor, null, null))
      .thenReturn(resolved);
    when(mockPluginConfig.getModuleUrlsMode()).thenReturn(ModuleUrlsMode.FALSE);
    when(mockMavenProject.getBuild()).thenReturn(mockBuild);
    when(mockBuild.getDirectory()).thenReturn(OUTPUT_DIR);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockJsonProvider).readJsonFromFile(null, ApplicationDescriptor.class, false);
  }

  @Test
  @SneakyThrows
  void execute_positive_callsUpdateServiceWithModuleStrings() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.cmdModulesString = "mod-a:1.0.0,mod-b:2.0.0";
    mojo.cmdUiModulesString = "folio_ui:3.0.0";
    mojo.mavenProject = mockMavenProject;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);
    when(mockUpdateService.update(inputDescriptor, "mod-a:1.0.0,mod-b:2.0.0", "folio_ui:3.0.0"))
      .thenReturn(resolved);
    when(mockPluginConfig.getModuleUrlsMode()).thenReturn(ModuleUrlsMode.FALSE);
    when(mockMavenProject.getBuild()).thenReturn(mockBuild);
    when(mockBuild.getDirectory()).thenReturn(OUTPUT_DIR);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockUpdateService).update(inputDescriptor, "mod-a:1.0.0,mod-b:2.0.0", "folio_ui:3.0.0");
  }

  private void setupContextMocks() {
    when(mockContextBuilder.withLog(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withMavenSession(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withMavenProject(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withPluginConfig(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withModuleRegistries(any())).thenReturn(mockContextBuilder);
    when(mojo.buildApplicationContext()).thenReturn(mockGenericApplicationContext);
    when(mockGenericApplicationContext.getBean(JsonProvider.class)).thenReturn(mockJsonProvider);
    when(mockGenericApplicationContext.getBean(ApplicationDescriptorUpdateService.class)).thenReturn(mockUpdateService);
    when(mockGenericApplicationContext.getBean(PluginConfig.class)).thenReturn(mockPluginConfig);
  }
}
