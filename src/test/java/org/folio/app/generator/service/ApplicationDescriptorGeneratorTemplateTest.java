package org.folio.app.generator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.app.generator.model.types.ModuleType.BE;
import static org.folio.app.generator.model.types.ModuleType.UI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.configuration.SpringConfiguration;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.ModulesLoadResult;
import org.folio.app.generator.model.PreReleaseFilter;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.JsonConverter;
import org.folio.app.generator.utils.PluginConfig;
import org.folio.app.generator.validator.ApplicationDependencyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ApplicationDescriptorGeneratorTemplateTest {

  private static final String TEMPLATES_PATH = "src/test/resources/templates/";

  @TempDir
  Path tempDir;

  @Mock private MavenProject mavenProject;
  @Mock private PluginConfig pluginConfig;
  @Mock private ModuleDescriptorService moduleDescriptorService;
  @Mock private ModuleVersionService moduleVersionService;
  @Mock private ApplicationDependencyValidator dependencyValidator;
  @Mock private Log log;
  @Mock private Build build;

  private ApplicationDescriptorGenerator generator;
  private ApplicationDescriptorService applicationDescriptorService;
  private JsonProvider jsonProvider;
  private JsonConverter jsonConverter;

  @BeforeEach
  void setUp() throws Exception {
    var objectMapper = new SpringConfiguration().objectMapper();

    jsonConverter = new JsonConverter(objectMapper);

    lenient().when(mavenProject.getName()).thenReturn("test-app");
    lenient().when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");
    lenient().when(mavenProject.getGroupId()).thenReturn("org.folio");
    lenient().when(mavenProject.getDescription()).thenReturn("Test application description");
    lenient().when(mavenProject.getBuild()).thenReturn(build);
    lenient().when(build.getDirectory()).thenReturn(tempDir.toString());

    jsonProvider = new JsonProvider(log, jsonConverter, mavenProject);

    applicationDescriptorService = new ApplicationDescriptorService(
      mavenProject, pluginConfig, moduleDescriptorService, moduleVersionService);

    generator = new ApplicationDescriptorGenerator(
      mavenProject, jsonProvider, applicationDescriptorService, dependencyValidator);

    lenient().when(moduleVersionService.resolveModulesConstraints(anyList(), eq(BE)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(moduleVersionService.resolveModulesConstraints(anyList(), eq(UI)))
      .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @SneakyThrows
  void generate_positive_minimalTemplateWithLatestVersions() {
    var resolvedBeModules = List.of(
      new Dependency("mod-configuration", "5.10.0-SNAPSHOT.123", PreReleaseFilter.TRUE),
      new Dependency("mod-permissions", "6.5.0-SNAPSHOT.456", PreReleaseFilter.TRUE),
      new Dependency("mod-users", "19.3.0-SNAPSHOT.789", PreReleaseFilter.TRUE));
    var resolvedUiModules = List.of(
      new Dependency("folio_developer", "7.1.0", PreReleaseFilter.TRUE),
      new Dependency("folio_tags", "4.0.0", PreReleaseFilter.TRUE));
    when(moduleVersionService.resolveModulesConstraints(anyList(), eq(BE))).thenReturn(resolvedBeModules);
    when(moduleVersionService.resolveModulesConstraints(anyList(), eq(UI))).thenReturn(resolvedUiModules);
    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(
        List.of(
          moduleDefinition("mod-configuration", "5.10.0-SNAPSHOT.123"),
          moduleDefinition("mod-permissions", "6.5.0-SNAPSHOT.456"),
          moduleDefinition("mod-users", "19.3.0-SNAPSHOT.789")),
        List.of(
          Map.of("id", "mod-configuration-5.10.0-SNAPSHOT.123"),
          Map.of("id", "mod-permissions-6.5.0-SNAPSHOT.456"),
          Map.of("id", "mod-users-19.3.0-SNAPSHOT.789"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(
        List.of(
          moduleDefinition("folio_developer", "7.1.0"),
          moduleDefinition("folio_tags", "4.0.0")),
        List.of(
          Map.of("id", "folio_developer-7.1.0"),
          Map.of("id", "folio_tags-4.0.0"))));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);
    doNothing().when(dependencyValidator).validateDependencies(any());

    var template = readTemplate("template-with-latest-versions.json", true);
    generator.generate(template);

    var outputFile = new File(tempDir.toFile(), "test-app-1.0.0-SNAPSHOT.json");
    assertThat(outputFile).exists();
    var result = jsonConverter.parse(outputFile, ApplicationDescriptor.class);
    assertThat(result.getName()).isEqualTo("test-app");
    assertThat(result.getVersion()).isEqualTo("1.0.0-SNAPSHOT");
    assertThat(result.getModules()).hasSize(3);
    assertThat(result.getUiModules()).hasSize(2);
    assertThat(result.getDependencies()).isEmpty();
    assertThat(result.getModules())
      .extracting(ModuleDefinition::getVersion)
      .containsExactlyInAnyOrder("5.10.0-SNAPSHOT.123", "6.5.0-SNAPSHOT.456", "19.3.0-SNAPSHOT.789");
  }

  @Test
  @SneakyThrows
  void generate_positive_lockFileWithConcreteVersions() {
    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(
        List.of(moduleDefinition("mod-finc-config", "7.0.0-SNAPSHOT.170")),
        List.of(Map.of("id", "mod-finc-config-7.0.0-SNAPSHOT.170"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(
        List.of(
          moduleDefinition("folio_finc-config", "8.0.1099000000000354"),
          moduleDefinition("folio_finc-select", "8.1.1099000000000335")),
        List.of(
          Map.of("id", "folio_finc-config-8.0.1099000000000354"),
          Map.of("id", "folio_finc-select-8.1.1099000000000335"))));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);
    doNothing().when(dependencyValidator).validateDependencies(any());

    var template = readTemplate("template-with-concrete-versions.json", false);
    generator.generate(template);

    var outputFile = new File(tempDir.toFile(), "app-finc-1.1.0-SNAPSHOT.100200000001741.json");
    assertThat(outputFile).exists();
    var result = jsonConverter.parse(outputFile, ApplicationDescriptor.class);
    assertThat(result.getName()).isEqualTo("app-finc");
    assertThat(result.getVersion()).isEqualTo("1.1.0-SNAPSHOT.100200000001741");
    assertThat(result.getModules()).hasSize(1);
    assertThat(result.getModules().get(0).getVersion()).isEqualTo("7.0.0-SNAPSHOT.170");
    assertThat(result.getUiModules()).hasSize(2);
    assertThat(result.getUiModules())
      .extracting(ModuleDefinition::getVersion)
      .containsExactlyInAnyOrder("8.0.1099000000000354", "8.1.1099000000000335");
    assertThat(result.getDependencies()).hasSize(1);
    assertThat(result.getDependencies().get(0).getName()).isEqualTo("app-acquisitions");
  }

  @Test
  @SneakyThrows
  void generate_positive_templateWithPreReleaseFilter() {
    var resolvedBeModules = List.of(
      new Dependency("mod-organizations", "2.2.0-SNAPSHOT.100", PreReleaseFilter.ONLY),
      new Dependency("mod-orders", "13.1.0-SNAPSHOT.200", PreReleaseFilter.ONLY),
      new Dependency("mod-invoice", "6.1.0-SNAPSHOT.300", PreReleaseFilter.ONLY));
    var resolvedUiModules = List.of(
      new Dependency("folio_organizations", "5.0.0-SNAPSHOT.400", PreReleaseFilter.ONLY),
      new Dependency("folio_orders", "6.0.0-SNAPSHOT.500", PreReleaseFilter.ONLY));
    when(moduleVersionService.resolveModulesConstraints(anyList(), eq(BE))).thenReturn(resolvedBeModules);
    when(moduleVersionService.resolveModulesConstraints(anyList(), eq(UI))).thenReturn(resolvedUiModules);
    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(
        List.of(
          moduleDefinition("mod-organizations", "2.2.0-SNAPSHOT.100"),
          moduleDefinition("mod-orders", "13.1.0-SNAPSHOT.200"),
          moduleDefinition("mod-invoice", "6.1.0-SNAPSHOT.300")),
        List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(
        List.of(
          moduleDefinition("folio_organizations", "5.0.0-SNAPSHOT.400"),
          moduleDefinition("folio_orders", "6.0.0-SNAPSHOT.500")),
        List.of()));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);
    doNothing().when(dependencyValidator).validateDependencies(any());

    var template = readTemplate("template-with-prerelease-filter.json", true);
    assertThat(template.getModules()).allSatisfy(module ->
      assertThat(module.getPreRelease()).isEqualTo(PreReleaseFilter.ONLY));
    assertThat(template.getUiModules()).allSatisfy(module ->
      assertThat(module.getPreRelease()).isEqualTo(PreReleaseFilter.ONLY));
    generator.generate(template);

    var outputFile = new File(tempDir.toFile(), "test-app-1.0.0-SNAPSHOT.json");
    assertThat(outputFile).exists();
    var result = jsonConverter.parse(outputFile, ApplicationDescriptor.class);
    assertThat(result.getModules())
      .extracting(ModuleDefinition::getVersion)
      .allMatch(v -> v.contains("SNAPSHOT"));
    assertThat(result.getUiModules())
      .extracting(ModuleDefinition::getVersion)
      .allMatch(v -> v.contains("SNAPSHOT"));
    assertThat(result.getDependencies()).hasSize(1);
    assertThat(result.getDependencies().get(0).getName()).isEqualTo("app-platform-complete");
  }

  @Test
  @SneakyThrows
  void generate_positive_withBuildNumber() {
    when(pluginConfig.getBuildNumber()).thenReturn("12345");
    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);
    doNothing().when(dependencyValidator).validateDependencies(any());

    var template = readTemplate("template-with-latest-versions.json", true);
    generator.generate(template);

    var outputFile = new File(tempDir.toFile(), "test-app-1.0.0-SNAPSHOT.12345.json");
    assertThat(outputFile).exists();
    var result = jsonConverter.parse(outputFile, ApplicationDescriptor.class);
    assertThat(result.getVersion()).isEqualTo("1.0.0-SNAPSHOT.12345");
    assertThat(result.getId()).isEqualTo("test-app-1.0.0-SNAPSHOT.12345");
  }

  @Test
  @SneakyThrows
  void generate_positive_withModuleDescriptorsIncluded() {
    var beModule = moduleDefinition("mod-configuration", "5.10.0");
    beModule.setUrl("http://registry/mod-configuration");
    var uiModule = moduleDefinition("folio_developer", "7.1.0");
    uiModule.setUrl("http://registry/folio_developer");
    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(
        List.of(beModule),
        List.of(Map.of("id", "mod-configuration-5.10.0", "name", "Configuration"))));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(
        List.of(uiModule),
        List.of(Map.of("id", "folio_developer-7.1.0", "name", "Developer"))));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(false);
    doNothing().when(dependencyValidator).validateDependencies(any());

    var template = readTemplate("template-with-latest-versions.json", true);
    generator.generate(template);

    var outputFile = new File(tempDir.toFile(), "test-app-1.0.0-SNAPSHOT.json");
    assertThat(outputFile).exists();
    var result = jsonConverter.parse(outputFile, ApplicationDescriptor.class);
    assertThat(result.getModuleDescriptors()).hasSize(1);
    assertThat(result.getModuleDescriptors().get(0)).containsEntry("id", "mod-configuration-5.10.0");
    assertThat(result.getUiModuleDescriptors()).hasSize(1);
    assertThat(result.getUiModuleDescriptors().get(0)).containsEntry("id", "folio_developer-7.1.0");
    assertThat(result.getModules().get(0).getUrl()).isNull();
    assertThat(result.getUiModules().get(0).getUrl()).isNull();
  }

  @Test
  @SneakyThrows
  void generate_positive_withUnknownPlatformField() {
    when(moduleDescriptorService.loadModules(eq(BE), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(moduleDescriptorService.loadModules(eq(UI), anyList())).thenReturn(
      new ModulesLoadResult(List.of(), List.of()));
    when(pluginConfig.isModuleUrlsOnly()).thenReturn(true);
    doNothing().when(dependencyValidator).validateDependencies(any());

    var template = readTemplate("template-with-unknown-platform-field.json", true);
    generator.generate(template);

    var outputFile = new File(tempDir.toFile(), "test-app-1.0.0-SNAPSHOT.json");
    assertThat(outputFile).exists();
    var result = jsonConverter.parse(outputFile, ApplicationDescriptor.class);
    assertThat(result.getName()).isEqualTo("test-app");
    assertThat(result.getVersion()).isEqualTo("1.0.0-SNAPSHOT");
  }

  @SneakyThrows
  private ApplicationDescriptorTemplate readTemplate(String fileName, boolean useSubstitution) {
    var path = TEMPLATES_PATH + fileName;
    return jsonProvider.readJsonFromFile(path, ApplicationDescriptorTemplate.class, useSubstitution);
  }

  private ModuleDefinition moduleDefinition(String name, String version) {
    return new ModuleDefinition()
      .id(name + "-" + version)
      .name(name)
      .version(version);
  }
}
