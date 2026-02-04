package org.folio.app.generator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import lombok.SneakyThrows;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ErrorDetail;
import org.folio.app.generator.model.ExecutionResult;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.UpdateConfig;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.service.ApplicationDescriptorUpdateService;
import org.folio.app.generator.service.JsonProvider;
import org.folio.app.generator.service.ModuleRegistryProvider;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.GenericApplicationContext;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UpdateGeneratorTest {

  @Mock private ModuleRegistryProvider mockRegistryProvider;
  @Mock private ApplicationContextBuilder mockContextBuilder;
  @Mock private GenericApplicationContext mockGenericApplicationContext;
  @Mock private ApplicationDescriptorUpdateService mockUpdateService;
  @Mock private JsonProvider mockJsonProvider;
  @Mock private MavenProject mavenProject;
  @Mock private Build build;
  @Captor private ArgumentCaptor<UpdateConfig> configCaptor;
  @InjectMocks private UpdateGenerator mojo;

  private ApplicationDescriptor inputDescriptor;

  @BeforeEach
  void setUp() {
    mojo.mavenProject = mavenProject;
    inputDescriptor = new ApplicationDescriptor()
      .id("test-app-1.0.0-SNAPSHOT")
      .name("test-app")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(new ModuleDefinition().id("mod-test-1.0.0").name("mod-test").version("1.0.0")))
      .uiModules(List.of(new ModuleDefinition().id("folio_test-2.0.0").name("folio_test").version("2.0.0")));
  }

  @Test
  @SneakyThrows
  void execute_positive_defaultParameters() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.cmdModulesString = "mod-orders:1.0.0,mod-users:2.0.0";
    mojo.cmdUiModulesString = "folio_orders:1.0.0";
    mojo.allowDowngrade = false;
    mojo.allowAddModules = false;
    mojo.removeUnlistedModules = false;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockUpdateService).update(eq(inputDescriptor), eq("mod-orders:1.0.0,mod-users:2.0.0"),
      eq("folio_orders:1.0.0"), configCaptor.capture());
    var config = configCaptor.getValue();
    assert !config.isAllowDowngrade();
    assert !config.isAllowAddModules();
    assert !config.isRemoveUnlistedModules();
  }

  @Test
  @SneakyThrows
  void execute_positive_allFlagsEnabled() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.cmdModulesString = "mod-orders:1.0.0";
    mojo.cmdUiModulesString = null;
    mojo.allowDowngrade = true;
    mojo.allowAddModules = true;
    mojo.removeUnlistedModules = true;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockUpdateService).update(eq(inputDescriptor), eq("mod-orders:1.0.0"),
      eq(null), configCaptor.capture());
    var config = configCaptor.getValue();
    assert config.isAllowDowngrade();
    assert config.isAllowAddModules();
    assert config.isRemoveUnlistedModules();
  }

  @Test
  @SneakyThrows
  void execute_positive_noVersionBumpEnabled() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.cmdModulesString = "mod-orders:1.0.0";
    mojo.cmdUiModulesString = null;
    mojo.allowDowngrade = false;
    mojo.allowAddModules = false;
    mojo.removeUnlistedModules = false;
    mojo.noVersionBump = true;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockUpdateService).update(eq(inputDescriptor), eq("mod-orders:1.0.0"),
      eq(null), configCaptor.capture());
    var config = configCaptor.getValue();
    assert config.isNoVersionBump();
  }

  @Test
  @SneakyThrows
  void execute_positive_nullModuleStrings() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.cmdModulesString = null;
    mojo.cmdUiModulesString = null;
    mojo.allowDowngrade = false;
    mojo.allowAddModules = false;
    mojo.removeUnlistedModules = false;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockUpdateService).update(eq(inputDescriptor), eq((String) null), eq((String) null),
      any(UpdateConfig.class));
  }

  @Test
  @SneakyThrows
  void execute_positive_usesDefaultPath() {
    mojo.allowDowngrade = true;
    mojo.allowAddModules = true;
    mojo.removeUnlistedModules = true;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile(null, ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockJsonProvider).readJsonFromFile(null, ApplicationDescriptor.class, false);
  }

  @Test
  @SneakyThrows
  void execute_negative_applicationGeneratorException() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.cmdModulesString = "mod-orders:1.0.0";
    mojo.cmdUiModulesString = null;

    var errorDetail = ErrorDetail.moduleNotFound("mod-missing", "1.0.0", "^1.0.0");
    var exception = new ApplicationGeneratorException("Module not found", ErrorCategory.MODULE_NOT_FOUND, errorDetail);

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);
    doThrow(exception).when(mockUpdateService).update(any(), any(String.class), any(), any());

    assertThatThrownBy(() -> mojo.execute())
      .isInstanceOf(MojoExecutionException.class)
      .hasMessage("Module not found")
      .hasCause(exception);

    verify(mockJsonProvider, times(2)).writeExecutionResult(any(ExecutionResult.class), eq("/target"));
  }

  @Test
  @SneakyThrows
  void execute_negative_genericException() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.cmdModulesString = "mod-orders:1.0.0";
    mojo.cmdUiModulesString = null;

    var exception = new RuntimeException("Unexpected error");

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);
    doThrow(exception).when(mockUpdateService).update(any(), any(String.class), any(), any());

    assertThatThrownBy(() -> mojo.execute())
      .isInstanceOf(MojoExecutionException.class)
      .hasMessage("Unexpected error")
      .hasCause(exception);

    verify(mockJsonProvider, times(2)).writeExecutionResult(any(ExecutionResult.class), eq("/target"));
  }

  private void setupContextMocks() {
    when(mavenProject.getArtifactId()).thenReturn("test-app");
    when(mavenProject.getBuild()).thenReturn(build);
    when(build.getDirectory()).thenReturn("/target");
    when(mockContextBuilder.withLog(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withMavenSession(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withMavenProject(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withPluginConfig(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withModuleRegistries(any())).thenReturn(mockContextBuilder);
    when(mojo.buildApplicationContext()).thenReturn(mockGenericApplicationContext);
    when(mockGenericApplicationContext.getBean(JsonProvider.class)).thenReturn(mockJsonProvider);
    when(mockGenericApplicationContext.getBean(ApplicationDescriptorUpdateService.class))
      .thenReturn(mockUpdateService);
  }
}
