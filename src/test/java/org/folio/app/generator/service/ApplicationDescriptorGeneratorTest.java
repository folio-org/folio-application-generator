package org.folio.app.generator.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.ResolvedApplicationDescriptor;
import org.folio.app.generator.model.types.ModuleUrlsMode;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.PluginConfig;
import org.folio.app.generator.validator.ApplicationDependencyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ApplicationDescriptorGeneratorTest {

  private static final String OUTPUT_DIR = "/target";
  private static final String APP_ID = "test-app-1.0.0";

  @Mock private MavenProject mavenProject;
  @Mock private Build build;
  @Mock private PluginConfig pluginConfig;
  @Mock private JsonProvider jsonProvider;
  @Mock private ApplicationDescriptorService applicationDescriptorService;
  @Mock private ApplicationDependencyValidator applicationDependencyValidator;
  @InjectMocks private ApplicationDescriptorGenerator generator;

  private ApplicationDescriptorTemplate template;
  private ResolvedApplicationDescriptor resolved;

  @BeforeEach
  void setUp() {
    template = new ApplicationDescriptorTemplate()
      .name("test-app")
      .version("1.0.0")
      .modules(List.of(new Dependency("mod-test", "1.0.0", null)))
      .uiModules(List.of(new Dependency("folio_test", "2.0.0", null)));

    resolved = ResolvedApplicationDescriptor.builder()
      .id(APP_ID)
      .name("test-app")
      .version("1.0.0")
      .modules(List.of(
        new ModuleDefinition().id("mod-test-1.0.0").name("mod-test").version("1.0.0").url("http://registry/mod-test")))
      .uiModules(List.of(
        new ModuleDefinition().id("folio_test-2.0.0").name("folio_test").version("2.0.0").url("http://registry/folio_test")))
      .moduleDescriptors(List.of(Map.of("id", "mod-test-1.0.0")))
      .uiModuleDescriptors(List.of(Map.of("id", "folio_test-2.0.0")))
      .dependencies(List.of(new Dependency("app-platform", "^1.0.0", null)))
      .build();
  }

  @Test
  @SneakyThrows
  void generate_positive_modeFalse_writesFullDescriptorOnly() {
    doNothing().when(applicationDependencyValidator).validateDependencies(template);
    when(applicationDescriptorService.create(template)).thenReturn(resolved);
    when(mavenProject.getBuild()).thenReturn(build);
    when(build.getDirectory()).thenReturn(OUTPUT_DIR);
    when(pluginConfig.getModuleUrlsMode()).thenReturn(ModuleUrlsMode.FALSE);

    generator.generate(template);

    verify(applicationDependencyValidator).validateDependencies(template);
    verify(applicationDescriptorService).create(template);
    verify(jsonProvider).writeApplication(any(ApplicationDescriptor.class), eq(OUTPUT_DIR));
    verify(jsonProvider, never()).writeApplication(any(ApplicationDescriptor.class), any(), any());
  }

  @Test
  @SneakyThrows
  void generate_positive_modeTrue_writesUrlOnlyDescriptor() {
    doNothing().when(applicationDependencyValidator).validateDependencies(template);
    when(applicationDescriptorService.create(template)).thenReturn(resolved);
    when(mavenProject.getBuild()).thenReturn(build);
    when(build.getDirectory()).thenReturn(OUTPUT_DIR);
    when(pluginConfig.getModuleUrlsMode()).thenReturn(ModuleUrlsMode.TRUE);

    generator.generate(template);

    verify(applicationDependencyValidator).validateDependencies(template);
    verify(applicationDescriptorService).create(template);
    verify(jsonProvider, never()).writeApplication(any(ApplicationDescriptor.class), eq(OUTPUT_DIR));
    verify(jsonProvider).writeApplication(any(ApplicationDescriptor.class), eq(OUTPUT_DIR), eq(APP_ID + ".json"));
  }

  @Test
  @SneakyThrows
  void generate_positive_modeBoth_writesBothDescriptors() {
    doNothing().when(applicationDependencyValidator).validateDependencies(template);
    when(applicationDescriptorService.create(template)).thenReturn(resolved);
    when(mavenProject.getBuild()).thenReturn(build);
    when(build.getDirectory()).thenReturn(OUTPUT_DIR);
    when(pluginConfig.getModuleUrlsMode()).thenReturn(ModuleUrlsMode.BOTH);

    generator.generate(template);

    verify(applicationDependencyValidator).validateDependencies(template);
    verify(applicationDescriptorService).create(template);
    verify(jsonProvider).writeApplication(any(ApplicationDescriptor.class), eq(OUTPUT_DIR));
    verify(jsonProvider).writeApplication(any(ApplicationDescriptor.class), eq(OUTPUT_DIR), eq(APP_ID + ".url.json"));
  }

  @Test
  @SneakyThrows
  void generate_positive_validatesTemplateBeforeCreation() {
    doNothing().when(applicationDependencyValidator).validateDependencies(template);
    when(applicationDescriptorService.create(template)).thenReturn(resolved);
    when(mavenProject.getBuild()).thenReturn(build);
    when(build.getDirectory()).thenReturn(OUTPUT_DIR);
    when(pluginConfig.getModuleUrlsMode()).thenReturn(ModuleUrlsMode.FALSE);

    generator.generate(template);

    verify(applicationDependencyValidator).validateDependencies(template);
  }

  @Test
  @SneakyThrows
  void generate_positive_modeFalse_fullDescriptorHasNoUrls() {
    doNothing().when(applicationDependencyValidator).validateDependencies(template);
    when(applicationDescriptorService.create(template)).thenReturn(resolved);
    when(mavenProject.getBuild()).thenReturn(build);
    when(build.getDirectory()).thenReturn(OUTPUT_DIR);
    when(pluginConfig.getModuleUrlsMode()).thenReturn(ModuleUrlsMode.FALSE);

    generator.generate(template);

    verify(jsonProvider).writeApplication(
      org.mockito.ArgumentMatchers.argThat(descriptor ->
        descriptor.getModules().get(0).getUrl() == null
          && descriptor.getUiModules().get(0).getUrl() == null
          && descriptor.getModuleDescriptors() != null),
      eq(OUTPUT_DIR));
  }

  @Test
  @SneakyThrows
  void generate_positive_modeTrue_urlOnlyDescriptorHasUrlsNoDescriptors() {
    doNothing().when(applicationDependencyValidator).validateDependencies(template);
    when(applicationDescriptorService.create(template)).thenReturn(resolved);
    when(mavenProject.getBuild()).thenReturn(build);
    when(build.getDirectory()).thenReturn(OUTPUT_DIR);
    when(pluginConfig.getModuleUrlsMode()).thenReturn(ModuleUrlsMode.TRUE);

    generator.generate(template);

    verify(jsonProvider).writeApplication(
      org.mockito.ArgumentMatchers.argThat(descriptor ->
        descriptor.getModules().get(0).getUrl() != null
          && descriptor.getUiModules().get(0).getUrl() != null
          && descriptor.getModuleDescriptors() == null),
      eq(OUTPUT_DIR),
      eq(APP_ID + ".json"));
  }
}
