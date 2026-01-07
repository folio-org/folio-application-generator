package org.folio.app.generator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.ModuleRegistries;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.service.loader.LoaderResultContainer;
import org.folio.app.generator.service.loader.ModuleDescriptorLoaderFacade;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.JsonConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ModuleDescriptorServiceTest {

  @InjectMocks private ModuleDescriptorService service;
  @Mock private Log log;
  @Mock private JsonConverter jsonConverter;
  @Mock private ModuleRegistries moduleRegistries;
  @Mock private ModuleDescriptorLoaderFacade moduleDescriptorLoaderFacade;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(log, jsonConverter);
  }

  @Test
  void loadModules_positive_allModulesFound() throws MalformedURLException {
    var module1 = moduleDefinition("mod-users", "1.0.0");
    var module2 = moduleDefinition("mod-orders", "2.0.0");
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(moduleDescriptorLoaderFacade.find(registry, module1))
      .thenReturn(Optional.of(loaderResult("mod-users", "1.0.0")));
    when(moduleDescriptorLoaderFacade.find(registry, module2))
      .thenReturn(Optional.of(loaderResult("mod-orders", "2.0.0")));

    var result = service.loadModules(ModuleType.BE, List.of(module1, module2));

    assertThat(result.artifacts()).hasSize(2);
    assertThat(result.descriptors()).hasSize(2);
  }

  @Test
  void loadModules_positive_emptyModuleList() {
    var registry = okapiRegistry();
    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));

    var result = service.loadModules(ModuleType.BE, List.of());

    assertThat(result.artifacts()).isEmpty();
    assertThat(result.descriptors()).isEmpty();
  }

  @Test
  void loadModules_positive_moduleAlreadyFoundSkipped() throws MalformedURLException {
    var module = moduleDefinition("mod-users", "1.0.0");
    var registry1 = okapiRegistry();
    var registry2 = new OkapiModuleRegistry().url("http://other").withGeneratedFields();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry1, registry2));
    when(moduleDescriptorLoaderFacade.find(registry1, module))
      .thenReturn(Optional.of(loaderResult("mod-users", "1.0.0")));

    var result = service.loadModules(ModuleType.BE, List.of(module));

    assertThat(result.artifacts()).hasSize(1);
  }

  @Test
  void loadModules_negative_emptyRegistriesWarning() {
    var module = moduleDefinition("mod-users", "1.0.0");

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of());

    assertThatThrownBy(() -> service.loadModules(ModuleType.BE, List.of(module)))
      .isInstanceOf(ApplicationGeneratorException.class)
      .satisfies(e -> {
        var ex = (ApplicationGeneratorException) e;
        assertThat(ex.getCategory()).isEqualTo(ErrorCategory.MODULE_NOT_FOUND);
      });

    verify(log).warn("Module registries are empty for type: BE");
  }

  @Test
  void loadModules_negative_moduleNotFound() throws MalformedURLException {
    var module1 = moduleDefinition("mod-users", "1.0.0");
    var module2 = moduleDefinition("mod-missing", "2.0.0");
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(moduleDescriptorLoaderFacade.find(registry, module1))
      .thenReturn(Optional.of(loaderResult("mod-users", "1.0.0")));
    when(moduleDescriptorLoaderFacade.find(registry, module2)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.loadModules(ModuleType.BE, List.of(module1, module2)))
      .isInstanceOf(ApplicationGeneratorException.class)
      .satisfies(e -> {
        var ex = (ApplicationGeneratorException) e;
        assertThat(ex.getCategory()).isEqualTo(ErrorCategory.MODULE_NOT_FOUND);
        assertThat(ex.getErrors()).hasSize(1);
        assertThat(ex.getErrors().get(0).artifact()).isEqualTo("mod-missing-2.0.0");
      });
  }

  @Test
  void loadModules_negative_multipleModulesNotFound() {
    var module1 = moduleDefinition("mod-missing1", "1.0.0");
    var module2 = moduleDefinition("mod-missing2", "2.0.0");
    var registry = okapiRegistry();

    when(moduleRegistries.getRegistries(ModuleType.BE)).thenReturn(List.of(registry));
    when(moduleDescriptorLoaderFacade.find(any(), any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.loadModules(ModuleType.BE, List.of(module1, module2)))
      .isInstanceOf(ApplicationGeneratorException.class)
      .satisfies(e -> {
        var ex = (ApplicationGeneratorException) e;
        assertThat(ex.getCategory()).isEqualTo(ErrorCategory.MODULE_NOT_FOUND);
        assertThat(ex.getErrors()).hasSize(2);
      });
  }

  @Test
  void convertToArtifact_positive() throws MalformedURLException {
    var loaderResult = loaderResult("mod-users", "1.0.0");

    var result = service.convertToArtifact(loaderResult);

    assertThat(result.getName()).isEqualTo("mod-users");
    assertThat(result.getVersion()).isEqualTo("1.0.0");
    assertThat(result.getUrl()).isEqualTo("http://localhost");
  }

  @Test
  void convertToArtifact_negative_invalidId() throws MalformedURLException {
    var loaderResult = new LoaderResultContainer()
      .moduleDescriptor(Map.of("id", 123, "name", "test"))
      .sourceUrl(new URL("http://localhost"));

    when(jsonConverter.toJsonString(any())).thenReturn("{\"id\": 123}");

    assertThatThrownBy(() -> service.convertToArtifact(loaderResult))
      .isInstanceOf(ApplicationGeneratorException.class)
      .hasMessageContaining("Loaded module id is invalid")
      .satisfies(e -> {
        var ex = (ApplicationGeneratorException) e;
        assertThat(ex.getCategory()).isEqualTo(ErrorCategory.CONFIGURATION_ERROR);
      });
  }

  @Test
  void convertToArtifact_negative_invalidModuleIdFormat() throws MalformedURLException {
    var loaderResult = new LoaderResultContainer()
      .moduleDescriptor(Map.of("id", "invalid-module-no-version"))
      .sourceUrl(new URL("http://localhost"));

    when(jsonConverter.toJsonString(any())).thenReturn("{\"id\": \"invalid-module-no-version\"}");

    assertThatThrownBy(() -> service.convertToArtifact(loaderResult))
      .isInstanceOf(ApplicationGeneratorException.class)
      .hasMessageContaining("Module cannot be created for a module descriptor")
      .satisfies(e -> {
        var ex = (ApplicationGeneratorException) e;
        assertThat(ex.getCategory()).isEqualTo(ErrorCategory.CONFIGURATION_ERROR);
      });
  }

  private static ModuleDefinition moduleDefinition(String name, String version) {
    return new ModuleDefinition()
      .id(name + "-" + version)
      .name(name)
      .version(version);
  }

  private static OkapiModuleRegistry okapiRegistry() {
    return new OkapiModuleRegistry().url("http://localhost").withGeneratedFields();
  }

  private static LoaderResultContainer loaderResult(String name, String version)
      throws MalformedURLException {
    return new LoaderResultContainer()
      .moduleDescriptor(Map.of("id", name + "-" + version, "name", name))
      .sourceUrl(new URL("http://localhost"));
  }
}
