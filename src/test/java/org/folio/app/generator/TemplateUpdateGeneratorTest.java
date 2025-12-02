package org.folio.app.generator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import lombok.SneakyThrows;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.UpdateConfig;
import org.folio.app.generator.service.ApplicationDescriptorUpdateService;
import org.folio.app.generator.service.JsonProvider;
import org.folio.app.generator.service.ModuleRegistryProvider;
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
class TemplateUpdateGeneratorTest {

  @Mock private ModuleRegistryProvider mockRegistryProvider;
  @Mock private ApplicationContextBuilder mockContextBuilder;
  @Mock private GenericApplicationContext mockGenericApplicationContext;
  @Mock private ApplicationDescriptorUpdateService mockUpdateService;
  @Mock private JsonProvider mockJsonProvider;
  @Captor private ArgumentCaptor<UpdateConfig> configCaptor;
  @InjectMocks private TemplateUpdateGenerator mojo;

  private ApplicationDescriptor inputDescriptor;
  private ApplicationDescriptorTemplate template;

  @BeforeEach
  void setUp() {
    inputDescriptor = new ApplicationDescriptor()
      .id("test-app-1.0.0-SNAPSHOT")
      .name("test-app")
      .version("1.0.0-SNAPSHOT")
      .modules(List.of(new ModuleDefinition().id("mod-test-1.0.0").name("mod-test").version("1.0.0")))
      .uiModules(List.of(new ModuleDefinition().id("folio_test-2.0.0").name("folio_test").version("2.0.0")));

    template = new ApplicationDescriptorTemplate()
      .modules(List.of(new Dependency("mod-test", "^1.0.0", null), new Dependency("mod-new", "1.0.0", null)))
      .uiModules(List.of(new Dependency("folio_test", "^2.0.0", null)));
  }

  @Test
  @SneakyThrows
  void execute_positive_defaultParameters() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.templatePath = "/path/to/template.json";
    mojo.allowDowngrade = true;
    mojo.allowAddModules = true;
    mojo.removeUnlistedModules = true;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);
    when(mockJsonProvider.readJsonFromFile("/path/to/template.json", ApplicationDescriptorTemplate.class, true))
      .thenReturn(template);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockUpdateService).update(eq(inputDescriptor), anyList(), anyList(), configCaptor.capture());
    var config = configCaptor.getValue();
    assert config.isAllowDowngrade();
    assert config.isAllowAddModules();
    assert config.isRemoveUnlistedModules();
  }

  @Test
  @SneakyThrows
  void execute_positive_customParameters() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.templatePath = "/path/to/template.json";
    mojo.allowDowngrade = false;
    mojo.allowAddModules = false;
    mojo.removeUnlistedModules = false;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);
    when(mockJsonProvider.readJsonFromFile("/path/to/template.json", ApplicationDescriptorTemplate.class, true))
      .thenReturn(template);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockUpdateService).update(eq(inputDescriptor), anyList(), anyList(), configCaptor.capture());
    var config = configCaptor.getValue();
    assert !config.isAllowDowngrade();
    assert !config.isAllowAddModules();
    assert !config.isRemoveUnlistedModules();
  }

  @Test
  @SneakyThrows
  void execute_positive_templateDependenciesOverride() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.templatePath = "/path/to/template.json";
    mojo.allowDowngrade = true;
    mojo.allowAddModules = true;
    mojo.removeUnlistedModules = true;

    inputDescriptor.setDependencies(List.of(new Dependency("app-old", "1.0.0", null)));
    template.setDependencies(List.of(new Dependency("app-new", "2.0.0", null)));

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);
    when(mockJsonProvider.readJsonFromFile("/path/to/template.json", ApplicationDescriptorTemplate.class, true))
      .thenReturn(template);

    assertDoesNotThrow(() -> mojo.execute());

    assert inputDescriptor.getDependencies().size() == 1;
    assert inputDescriptor.getDependencies().get(0).getName().equals("app-new");
  }

  @Test
  @SneakyThrows
  void execute_positive_usesDefaultPaths() {
    mojo.allowDowngrade = true;
    mojo.allowAddModules = true;
    mojo.removeUnlistedModules = true;

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile(null, ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);
    when(mockJsonProvider.readJsonFromFile(null, ApplicationDescriptorTemplate.class, true))
      .thenReturn(template);

    assertDoesNotThrow(() -> mojo.execute());

    verify(mockJsonProvider).readJsonFromFile(null, ApplicationDescriptor.class, false);
    verify(mockJsonProvider).readJsonFromFile(null, ApplicationDescriptorTemplate.class, true);
  }

  @Test
  @SneakyThrows
  void execute_positive_nullTemplateDependenciesResultsInEmptyList() {
    mojo.appDescriptorPath = "/path/to/descriptor.json";
    mojo.templatePath = "/path/to/template.json";
    mojo.allowDowngrade = true;
    mojo.allowAddModules = true;
    mojo.removeUnlistedModules = true;

    inputDescriptor.setDependencies(List.of(new Dependency("app-old", "1.0.0", null)));
    template.setDependencies(null);

    setupContextMocks();
    when(mockJsonProvider.readJsonFromFile("/path/to/descriptor.json", ApplicationDescriptor.class, false))
      .thenReturn(inputDescriptor);
    when(mockJsonProvider.readJsonFromFile("/path/to/template.json", ApplicationDescriptorTemplate.class, true))
      .thenReturn(template);

    assertDoesNotThrow(() -> mojo.execute());

    assert inputDescriptor.getDependencies().isEmpty();
  }

  private void setupContextMocks() {
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
