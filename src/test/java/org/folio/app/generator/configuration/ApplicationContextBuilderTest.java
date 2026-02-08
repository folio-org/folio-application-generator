package org.folio.app.generator.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.app.generator.model.registry.ModuleRegistries;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.PluginConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ApplicationContextBuilderTest {

  @Mock private PluginConfig pluginConfig;

  private AnnotationConfigApplicationContext context;

  @BeforeEach
  void setUp() {
    context = new AnnotationConfigApplicationContext();
  }

  @AfterEach
  void tearDown() {
    var properties = context.getEnvironment().getSystemProperties();
    for (var artifactRegistryType : ArtifactRegistryType.values()) {
      properties.remove(artifactRegistryType.getPropertyName());
    }
    for (var registryType : RegistryType.values()) {
      properties.remove(registryType.getPropertyName());
    }
    context.close();
  }

  @Test
  void setSpringContextProperties_positive_validateArtifactsEnabled() {
    when(pluginConfig.isValidateArtifacts()).thenReturn(true);
    var moduleRegistries = new ModuleRegistries(List.of(), List.of(), List.of(), List.of());

    var builder = new ApplicationContextBuilder()
      .withPluginConfig(pluginConfig)
      .withModuleRegistries(moduleRegistries);

    builder.setSpringContextProperties(context);

    var properties = context.getEnvironment().getSystemProperties();
    for (var artifactRegistryType : ArtifactRegistryType.values()) {
      assertThat(properties).containsEntry(artifactRegistryType.getPropertyName(), true);
    }
  }

  @Test
  void setSpringContextProperties_positive_validateArtifactsDisabled() {
    when(pluginConfig.isValidateArtifacts()).thenReturn(false);
    var moduleRegistries = new ModuleRegistries(List.of(), List.of(), List.of(), List.of());

    var builder = new ApplicationContextBuilder()
      .withPluginConfig(pluginConfig)
      .withModuleRegistries(moduleRegistries);

    builder.setSpringContextProperties(context);

    var properties = context.getEnvironment().getSystemProperties();
    for (var artifactRegistryType : ArtifactRegistryType.values()) {
      assertThat(properties).doesNotContainKey(artifactRegistryType.getPropertyName());
    }
  }

  @Test
  void setSpringContextProperties_positive_withModuleRegistries() {
    when(pluginConfig.isValidateArtifacts()).thenReturn(false);
    var okapiRegistry = new OkapiModuleRegistry().url("http://localhost:9130");
    var moduleRegistries = new ModuleRegistries(List.of(okapiRegistry), List.of(), List.of(), List.of());

    var builder = new ApplicationContextBuilder()
      .withPluginConfig(pluginConfig)
      .withModuleRegistries(moduleRegistries);

    builder.setSpringContextProperties(context);

    var properties = context.getEnvironment().getSystemProperties();
    assertThat(properties).containsEntry(RegistryType.OKAPI.getPropertyName(), true);
  }
}
