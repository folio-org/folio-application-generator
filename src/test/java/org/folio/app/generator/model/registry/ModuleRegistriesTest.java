package org.folio.app.generator.model.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ModuleRegistriesTest {

  @Test
  void getRegistries_positive_beType() {
    var beRegistry = new OkapiModuleRegistry().url("http://be").withGeneratedFields();
    var uiRegistry = new OkapiModuleRegistry().url("http://ui").withGeneratedFields();
    var registries = new ModuleRegistries(
      List.of(beRegistry), List.of(uiRegistry), List.of(), List.of());

    var result = registries.getRegistries(ModuleType.BE);

    assertThat(result).containsExactly(beRegistry);
  }

  @Test
  void getRegistries_positive_uiType() {
    var beRegistry = new OkapiModuleRegistry().url("http://be").withGeneratedFields();
    var uiRegistry = new OkapiModuleRegistry().url("http://ui").withGeneratedFields();
    var registries = new ModuleRegistries(
      List.of(beRegistry), List.of(uiRegistry), List.of(), List.of());

    var result = registries.getRegistries(ModuleType.UI);

    assertThat(result).containsExactly(uiRegistry);
  }

  @Test
  void getFallbackRegistries_positive_beType() {
    var beFallback = new OkapiModuleRegistry().url("http://be-fallback").withGeneratedFields();
    var uiFallback = new OkapiModuleRegistry().url("http://ui-fallback").withGeneratedFields();
    var registries = new ModuleRegistries(
      List.of(), List.of(), List.of(beFallback), List.of(uiFallback));

    var result = registries.getFallbackRegistries(ModuleType.BE);

    assertThat(result).containsExactly(beFallback);
  }

  @Test
  void getFallbackRegistries_positive_uiType() {
    var beFallback = new OkapiModuleRegistry().url("http://be-fallback").withGeneratedFields();
    var uiFallback = new OkapiModuleRegistry().url("http://ui-fallback").withGeneratedFields();
    var registries = new ModuleRegistries(
      List.of(), List.of(), List.of(beFallback), List.of(uiFallback));

    var result = registries.getFallbackRegistries(ModuleType.UI);

    assertThat(result).containsExactly(uiFallback);
  }
}
