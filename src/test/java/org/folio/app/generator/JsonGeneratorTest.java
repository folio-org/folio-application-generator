package org.folio.app.generator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.service.ApplicationDescriptorGenerator;
import org.folio.app.generator.service.JsonProvider;
import org.folio.app.generator.service.ModuleRegistryProvider;
import org.folio.app.generator.support.UnitTest;
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
  @Mock private ApplicationDescriptorGenerator mockApplicationDescriptorGenerator;
  @Mock private JsonProvider mockJsonProvider;
  @InjectMocks private JsonGenerator mojo;

  @Test
  @SneakyThrows
  void execute_positive_generatesFromTemplate() {
    mojo.templatePath = "/path/to/template.json";
    setupContextMocks();

    final var template = new ApplicationDescriptorTemplate().name("test-app").version("1.0.0");
    when(mockJsonProvider.readJsonFromFile("/path/to/template.json", ApplicationDescriptorTemplate.class, true))
      .thenReturn(template);
    doNothing().when(mockApplicationDescriptorGenerator).generate(any(ApplicationDescriptorTemplate.class));

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockApplicationDescriptorGenerator).generate(template);
  }

  @Test
  @SneakyThrows
  void execute_positive_usesDefaultTemplatePath() {
    setupContextMocks();

    final var template = new ApplicationDescriptorTemplate().name("test-app").version("1.0.0");
    when(mockJsonProvider.readJsonFromFile(null, ApplicationDescriptorTemplate.class, true))
      .thenReturn(template);
    doNothing().when(mockApplicationDescriptorGenerator).generate(any(ApplicationDescriptorTemplate.class));

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockJsonProvider).readJsonFromFile(null, ApplicationDescriptorTemplate.class, true);
  }

  @Test
  @SneakyThrows
  void readTemplate_positive_readsFromJsonProvider() {
    mojo.templatePath = "/custom/path/template.json";
    setupContextMocksForReadTemplate();

    final var template = new ApplicationDescriptorTemplate().name("test-app").version("1.0.0");
    when(mockJsonProvider.readJsonFromFile("/custom/path/template.json", ApplicationDescriptorTemplate.class, true))
      .thenReturn(template);

    assertDoesNotThrow(() -> {
      var result = mojo.readTemplate();
      assert result == template;
    });

    verify(mockJsonProvider).readJsonFromFile("/custom/path/template.json", ApplicationDescriptorTemplate.class, true);
  }

  private void setupContextMocks() {
    when(mockContextBuilder.withLog(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withMavenSession(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withMavenProject(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withPluginConfig(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withModuleRegistries(any())).thenReturn(mockContextBuilder);
    when(mojo.buildApplicationContext()).thenReturn(mockGenericApplicationContext);
    when(mockGenericApplicationContext.getBean(ApplicationDescriptorGenerator.class))
      .thenReturn(mockApplicationDescriptorGenerator);
    when(mockGenericApplicationContext.getBean(JsonProvider.class))
      .thenReturn(mockJsonProvider);
  }

  private void setupContextMocksForReadTemplate() {
    when(mockContextBuilder.withLog(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withMavenSession(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withMavenProject(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withPluginConfig(any())).thenReturn(mockContextBuilder);
    when(mockContextBuilder.withModuleRegistries(any())).thenReturn(mockContextBuilder);
    when(mojo.buildApplicationContext()).thenReturn(mockGenericApplicationContext);
    when(mockGenericApplicationContext.getBean(JsonProvider.class))
      .thenReturn(mockJsonProvider);
  }
}
