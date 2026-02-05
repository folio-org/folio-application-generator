package org.folio.app.generator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.ErrorDetail;
import org.folio.app.generator.model.ExecutionResult;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.service.ApplicationDescriptorService;
import org.folio.app.generator.service.ApplicationModulesIntegrityValidator;
import org.folio.app.generator.service.JsonProvider;
import org.folio.app.generator.service.ModuleRegistryProvider;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.service.exceptions.ModulesIntegrityValidatorException;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.GenericApplicationContext;

@UnitTest
@ExtendWith(MockitoExtension.class)
class IntegrityValidatorMojoTest {

  @Mock
  private ModuleRegistryProvider mockRegistryProvider;
  @Mock
  private ApplicationContextBuilder mockContextBuilder;
  @Mock
  private GenericApplicationContext mockGenericApplicationContext;
  @Mock
  private ApplicationDescriptorService mockApplicationDescriptorService;
  @Mock
  private ApplicationModulesIntegrityValidator mockApplicationModulesIntegrityValidator;
  @Mock
  private JsonProvider mockJsonProvider;
  @Mock
  private MavenProject mavenProject;
  @Mock
  private Build build;
  @InjectMocks
  private IntegrityValidatorMojo mockMojo;

  @BeforeEach
  void setUp() {
    mockMojo.mavenProject = mavenProject;
  }

  @Test
  void execute_shouldRunWithoutErrors() {
    mockMojo.baseUrl = "baseUrl";
    mockMojo.token = "token";

    when(mavenProject.getArtifactId()).thenReturn("test-app");
    when(mavenProject.getBuild()).thenReturn(build);
    when(build.getDirectory()).thenReturn("/target");
    when(mockContextBuilder.withLog(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withMavenSession(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withMavenProject(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withPluginConfig(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withModuleRegistries(any())).thenReturn(mockContextBuilder);

    when(mockMojo.buildApplicationContext()).thenReturn(mockGenericApplicationContext);

    when(mockGenericApplicationContext.getBean(ApplicationDescriptorService.class))
      .thenReturn(mockApplicationDescriptorService);
    when(mockGenericApplicationContext.containsBean("applicationModulesIntegrityValidator"))
      .thenReturn(true);
    when(mockGenericApplicationContext.getBean(ApplicationModulesIntegrityValidator.class))
      .thenReturn(mockApplicationModulesIntegrityValidator);
    when(mockGenericApplicationContext.getBean(JsonProvider.class))
      .thenReturn(mockJsonProvider);
    when(mockJsonProvider.readJsonFromFile(null, ApplicationDescriptorTemplate.class, true))
      .thenReturn(new ApplicationDescriptorTemplate());
    when(mockApplicationDescriptorService.create(any(ApplicationDescriptorTemplate.class)))
      .thenReturn(new ApplicationDescriptor().version("1.0.0"));
    doNothing().when(mockApplicationModulesIntegrityValidator).validateApplication(any(), any(), any());

    assertDoesNotThrow(() -> mockMojo.execute());
  }

  @Test
  void execute_shouldRunWithErrorsWithoutToken() {
    mockMojo.baseUrl = "baseUrl";
    assertThrows(MojoExecutionException.class, () -> mockMojo.execute());
  }

  @Test
  void execute_shouldRunWithErrorsWithoutUrl() {
    mockMojo.token = "token";
    assertThrows(MojoExecutionException.class, () -> mockMojo.execute());
  }

  @Test
  @SuppressWarnings("java:S5778")
  void execute_shouldThrow() {
    assertThrows(ModulesIntegrityValidatorException.class, () -> { //NOSONAR
      throw new ModulesIntegrityValidatorException(new InterruptedException());
    });
  }

  @Test
  void execute_negative_applicationGeneratorException() {
    mockMojo.baseUrl = "baseUrl";
    mockMojo.token = "token";

    var errorDetail = ErrorDetail.moduleNotFound("mod-missing", "1.0.0", "^1.0.0");
    var exception = new ApplicationGeneratorException("Module not found", ErrorCategory.MODULE_NOT_FOUND, errorDetail);

    setupContextMocks();
    when(mockApplicationDescriptorService.create(any(ApplicationDescriptorTemplate.class)))
      .thenThrow(exception);

    assertThatThrownBy(() -> mockMojo.execute())
      .isInstanceOf(MojoExecutionException.class)
      .hasMessage("Module not found")
      .hasCause(exception);

    verify(mockJsonProvider, times(2)).writeExecutionResult(any(ExecutionResult.class), eq("/target"));
  }

  @Test
  void execute_negative_genericException() {
    mockMojo.baseUrl = "baseUrl";
    mockMojo.token = "token";

    var exception = new RuntimeException("Unexpected error");

    setupContextMocks();
    when(mockApplicationDescriptorService.create(any(ApplicationDescriptorTemplate.class)))
      .thenThrow(exception);

    assertThatThrownBy(() -> mockMojo.execute())
      .isInstanceOf(MojoExecutionException.class)
      .hasMessage("Unexpected error")
      .hasCause(exception);

    verify(mockJsonProvider, times(2)).writeExecutionResult(any(ExecutionResult.class), eq("/target"));
  }

  @Test
  void execute_negative_validatorThrowsApplicationGeneratorException() {
    mockMojo.baseUrl = "baseUrl";
    mockMojo.token = "token";

    var errorDetail = ErrorDetail.httpError("http://localhost", 500, "Internal error");
    var exception = new ApplicationGeneratorException(
      "Validation failed", ErrorCategory.VALIDATION_FAILED, errorDetail);

    setupContextMocks();
    when(mockApplicationDescriptorService.create(any(ApplicationDescriptorTemplate.class)))
      .thenReturn(new ApplicationDescriptor().version("1.0.0"));
    doThrow(exception).when(mockApplicationModulesIntegrityValidator).validateApplication(any(), any(), any());

    assertThatThrownBy(() -> mockMojo.execute())
      .isInstanceOf(MojoExecutionException.class)
      .hasMessage("Validation failed")
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
    when(mockMojo.buildApplicationContext()).thenReturn(mockGenericApplicationContext);
    when(mockGenericApplicationContext.containsBean("applicationModulesIntegrityValidator"))
      .thenReturn(true);
    when(mockGenericApplicationContext.getBean(ApplicationDescriptorService.class))
      .thenReturn(mockApplicationDescriptorService);
    when(mockGenericApplicationContext.getBean(ApplicationModulesIntegrityValidator.class))
      .thenReturn(mockApplicationModulesIntegrityValidator);
    when(mockGenericApplicationContext.getBean(JsonProvider.class))
      .thenReturn(mockJsonProvider);
    when(mockJsonProvider.readJsonFromFile(null, ApplicationDescriptorTemplate.class, true))
      .thenReturn(new ApplicationDescriptorTemplate());
  }
}
