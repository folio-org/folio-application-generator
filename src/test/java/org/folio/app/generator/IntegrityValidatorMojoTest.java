package org.folio.app.generator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import org.apache.maven.plugin.MojoExecutionException;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.service.ApplicationDescriptorService;
import org.folio.app.generator.service.ApplicationModulesIntegrityValidator;
import org.folio.app.generator.service.JsonProvider;
import org.folio.app.generator.service.ModuleRegistryProvider;
import org.folio.app.generator.service.exceptions.ModulesIntegrityValidatorException;
import org.folio.app.generator.support.UnitTest;
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
  @InjectMocks
  private IntegrityValidatorMojo mockMojo;

  @Test
  void execute_shouldRunWithoutErrors() throws MojoExecutionException {
    mockMojo.baseUrl = "baseUrl";
    mockMojo.token = "token";

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
      .thenReturn(new ApplicationDescriptor());
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
}
