package org.folio.app.generator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.ErrorDetail;
import org.folio.app.generator.model.ExecutionResult;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.service.ApplicationDescriptorGenerator;
import org.folio.app.generator.service.JsonProvider;
import org.folio.app.generator.service.ModuleRegistryProvider;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
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
class JsonGeneratorTest {

  @Mock private ModuleRegistryProvider mockRegistryProvider;
  @Mock private ApplicationContextBuilder mockContextBuilder;
  @Mock private GenericApplicationContext mockGenericApplicationContext;
  @Mock private ApplicationDescriptorGenerator mockAppDescriptorGenerator;
  @Mock private JsonProvider mockJsonProvider;
  @Mock private MavenProject mavenProject;
  @Mock private Build build;
  @InjectMocks private JsonGenerator mojo;

  @BeforeEach
  void setUp() {
    mojo.mavenProject = mavenProject;
  }

  @Test
  @SneakyThrows
  void execute_positive() {
    mojo.templatePath = "/path/to/template.json";

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/template.json", ApplicationDescriptorTemplate.class, true))
      .thenReturn(new ApplicationDescriptorTemplate());
    var application = new ApplicationDescriptor().version("1.0.0");
    when(mockAppDescriptorGenerator.generate(any(ApplicationDescriptorTemplate.class))).thenReturn(application);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockAppDescriptorGenerator).generate(any(ApplicationDescriptorTemplate.class));
    verify(mockJsonProvider, times(2)).writeExecutionResult(any(ExecutionResult.class), eq("/target"));
  }

  @Test
  @SneakyThrows
  void execute_negative_applicationGeneratorException() {
    mojo.templatePath = "/path/to/template.json";
    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/template.json", ApplicationDescriptorTemplate.class, true))
      .thenReturn(new ApplicationDescriptorTemplate());

    var errorDetail = ErrorDetail.moduleNotFound("mod-missing", "1.0.0", "^1.0.0");
    var exception = new ApplicationGeneratorException("Module not found", ErrorCategory.MODULE_NOT_FOUND, errorDetail);
    doThrow(exception).when(mockAppDescriptorGenerator).generate(any());

    assertThatThrownBy(() -> mojo.execute())
      .isInstanceOf(MojoExecutionException.class)
      .hasMessage("Module not found")
      .hasCause(exception);

    verify(mockJsonProvider, times(2)).writeExecutionResult(any(ExecutionResult.class), eq("/target"));
  }

  @Test
  @SneakyThrows
  void execute_negative_genericException() {
    mojo.templatePath = "/path/to/template.json";
    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/template.json", ApplicationDescriptorTemplate.class, true))
      .thenReturn(new ApplicationDescriptorTemplate());

    var exception = new RuntimeException("Unexpected error");
    doThrow(exception).when(mockAppDescriptorGenerator).generate(any());

    assertThatThrownBy(() -> mojo.execute())
      .isInstanceOf(MojoExecutionException.class)
      .hasMessage("Unexpected error")
      .hasCause(exception);

    verify(mockJsonProvider, times(2)).writeExecutionResult(any(ExecutionResult.class), eq("/target"));
  }

  @Test
  @SneakyThrows
  void execute_negative_mojoExecutionExceptionRethrown() {
    mojo.templatePath = "/path/to/template.json";
    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/template.json", ApplicationDescriptorTemplate.class, true))
      .thenReturn(new ApplicationDescriptorTemplate());

    var originalException = new MojoExecutionException("Original mojo error");
    doThrow(originalException).when(mockAppDescriptorGenerator).generate(any());

    assertThatThrownBy(() -> mojo.execute())
      .isSameAs(originalException);

    verify(mockJsonProvider, times(2)).writeExecutionResult(any(ExecutionResult.class), eq("/target"));
  }

  @Test
  @SneakyThrows
  void execute_negative_templateReadException() {
    mojo.templatePath = "/path/to/template.json";
    setupContextMocks();

    var exception = new ApplicationGeneratorException("Template not found", ErrorCategory.CONFIGURATION_ERROR);
    when(mockJsonProvider.readJsonFromFile("/path/to/template.json", ApplicationDescriptorTemplate.class, true))
      .thenThrow(exception);

    assertThatThrownBy(() -> mojo.execute())
      .isInstanceOf(MojoExecutionException.class)
      .hasMessage("Template not found")
      .hasCause(exception);

    verify(mockJsonProvider, times(2)).writeExecutionResult(any(ExecutionResult.class), eq("/target"));
  }

  private void setupContextMocks() {
    when(mavenProject.getArtifactId()).thenReturn("test-app");
    lenient().when(mavenProject.getVersion()).thenReturn("1.0.0");
    when(mavenProject.getBuild()).thenReturn(build);
    when(build.getDirectory()).thenReturn("/target");
    when(mockContextBuilder.withLog(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withMavenSession(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withMavenProject(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withPluginConfig(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withModuleRegistries(any())).thenReturn(mockContextBuilder);
    when(mojo.buildApplicationContext()).thenReturn(mockGenericApplicationContext);
    when(mockGenericApplicationContext.getBean(JsonProvider.class)).thenReturn(mockJsonProvider);
    when(mockGenericApplicationContext.getBean(ApplicationDescriptorGenerator.class))
      .thenReturn(mockAppDescriptorGenerator);
  }
}
