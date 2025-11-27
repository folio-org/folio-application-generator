package org.folio.app.generator.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ResolvedApplicationDescriptorTest {

  @Test
  void toFullDescriptor_positive_includesModuleDescriptors() {
    var resolved = ResolvedApplicationDescriptor.builder()
      .id("test-app-1.0.0")
      .name("test-app")
      .version("1.0.0")
      .description("Test application")
      .platform("base")
      .modules(List.of(
        new ModuleDefinition().id("mod-test-1.0.0").name("mod-test").version("1.0.0").url("http://registry/mod-test")))
      .uiModules(List.of(
        new ModuleDefinition().id("folio_test-2.0.0").name("folio_test").version("2.0.0").url("http://registry/folio_test")))
      .moduleDescriptors(List.of(Map.of("id", "mod-test-1.0.0")))
      .uiModuleDescriptors(List.of(Map.of("id", "folio_test-2.0.0")))
      .dependencies(List.of(new Dependency("app-platform", "^1.0.0", null)))
      .build();

    var result = resolved.toFullDescriptor();

    assertThat(result.getId()).isEqualTo("test-app-1.0.0");
    assertThat(result.getName()).isEqualTo("test-app");
    assertThat(result.getVersion()).isEqualTo("1.0.0");
    assertThat(result.getDescription()).isEqualTo("Test application");
    assertThat(result.getPlatform()).isEqualTo("base");
    assertThat(result.getModuleDescriptors()).isEqualTo(List.of(Map.of("id", "mod-test-1.0.0")));
    assertThat(result.getUiModuleDescriptors()).isEqualTo(List.of(Map.of("id", "folio_test-2.0.0")));
    assertThat(result.getDependencies()).hasSize(1);
  }

  @Test
  void toFullDescriptor_positive_modulesWithoutUrls() {
    var resolved = ResolvedApplicationDescriptor.builder()
      .id("test-app-1.0.0")
      .name("test-app")
      .version("1.0.0")
      .modules(List.of(
        new ModuleDefinition().id("mod-test-1.0.0").name("mod-test").version("1.0.0").url("http://registry/mod-test")))
      .uiModules(List.of(
        new ModuleDefinition().id("folio_test-2.0.0").name("folio_test").version("2.0.0").url("http://registry/folio_test")))
      .build();

    var result = resolved.toFullDescriptor();

    assertThat(result.getModules()).hasSize(1);
    assertThat(result.getModules().get(0).getId()).isEqualTo("mod-test-1.0.0");
    assertThat(result.getModules().get(0).getUrl()).isNull();
    assertThat(result.getUiModules()).hasSize(1);
    assertThat(result.getUiModules().get(0).getId()).isEqualTo("folio_test-2.0.0");
    assertThat(result.getUiModules().get(0).getUrl()).isNull();
  }

  @Test
  void toUrlOnlyDescriptor_positive_noModuleDescriptors() {
    var resolved = ResolvedApplicationDescriptor.builder()
      .id("test-app-1.0.0")
      .name("test-app")
      .version("1.0.0")
      .description("Test application")
      .platform("base")
      .modules(List.of(
        new ModuleDefinition().id("mod-test-1.0.0").name("mod-test").version("1.0.0").url("http://registry/mod-test")))
      .uiModules(List.of(
        new ModuleDefinition().id("folio_test-2.0.0").name("folio_test").version("2.0.0").url("http://registry/folio_test")))
      .moduleDescriptors(List.of(Map.of("id", "mod-test-1.0.0")))
      .uiModuleDescriptors(List.of(Map.of("id", "folio_test-2.0.0")))
      .dependencies(List.of(new Dependency("app-platform", "^1.0.0", null)))
      .build();

    var result = resolved.toUrlOnlyDescriptor();

    assertThat(result.getId()).isEqualTo("test-app-1.0.0");
    assertThat(result.getName()).isEqualTo("test-app");
    assertThat(result.getVersion()).isEqualTo("1.0.0");
    assertThat(result.getDescription()).isEqualTo("Test application");
    assertThat(result.getPlatform()).isEqualTo("base");
    assertThat(result.getModuleDescriptors()).isNull();
    assertThat(result.getUiModuleDescriptors()).isNull();
    assertThat(result.getDependencies()).hasSize(1);
  }

  @Test
  void toUrlOnlyDescriptor_positive_modulesWithUrls() {
    var resolved = ResolvedApplicationDescriptor.builder()
      .id("test-app-1.0.0")
      .name("test-app")
      .version("1.0.0")
      .modules(List.of(
        new ModuleDefinition().id("mod-test-1.0.0").name("mod-test").version("1.0.0").url("http://registry/mod-test")))
      .uiModules(List.of(
        new ModuleDefinition().id("folio_test-2.0.0").name("folio_test").version("2.0.0").url("http://registry/folio_test")))
      .build();

    var result = resolved.toUrlOnlyDescriptor();

    assertThat(result.getModules()).hasSize(1);
    assertThat(result.getModules().get(0).getId()).isEqualTo("mod-test-1.0.0");
    assertThat(result.getModules().get(0).getUrl()).isEqualTo("http://registry/mod-test");
    assertThat(result.getUiModules()).hasSize(1);
    assertThat(result.getUiModules().get(0).getId()).isEqualTo("folio_test-2.0.0");
    assertThat(result.getUiModules().get(0).getUrl()).isEqualTo("http://registry/folio_test");
  }

  @Test
  void toFullDescriptor_positive_nullModules() {
    var resolved = ResolvedApplicationDescriptor.builder()
      .id("test-app-1.0.0")
      .name("test-app")
      .version("1.0.0")
      .modules(null)
      .uiModules(null)
      .build();

    var result = resolved.toFullDescriptor();

    assertThat(result.getModules()).isEmpty();
    assertThat(result.getUiModules()).isEmpty();
  }

  @Test
  void toUrlOnlyDescriptor_positive_nullModules() {
    var resolved = ResolvedApplicationDescriptor.builder()
      .id("test-app-1.0.0")
      .name("test-app")
      .version("1.0.0")
      .modules(null)
      .uiModules(null)
      .build();

    var result = resolved.toUrlOnlyDescriptor();

    assertThat(result.getModules()).isEmpty();
    assertThat(result.getUiModules()).isEmpty();
  }

  @Test
  void toFullDescriptor_positive_emptyModules() {
    var resolved = ResolvedApplicationDescriptor.builder()
      .id("test-app-1.0.0")
      .name("test-app")
      .version("1.0.0")
      .modules(List.of())
      .uiModules(List.of())
      .build();

    var result = resolved.toFullDescriptor();

    assertThat(result.getModules()).isEmpty();
    assertThat(result.getUiModules()).isEmpty();
  }

  @Test
  void toUrlOnlyDescriptor_positive_emptyModules() {
    var resolved = ResolvedApplicationDescriptor.builder()
      .id("test-app-1.0.0")
      .name("test-app")
      .version("1.0.0")
      .modules(List.of())
      .uiModules(List.of())
      .build();

    var result = resolved.toUrlOnlyDescriptor();

    assertThat(result.getModules()).isEmpty();
    assertThat(result.getUiModules()).isEmpty();
  }
}
