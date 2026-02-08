package org.folio.app.generator.model.registry;

import java.util.List;
import org.folio.app.generator.model.types.ModuleType;

public record ModuleRegistries(
    List<ModuleRegistry> beRegistries,
    List<ModuleRegistry> uiRegistries,
    List<ModuleRegistry> beFallbackRegistries,
    List<ModuleRegistry> uiFallbackRegistries) {

  public List<ModuleRegistry> getRegistries(ModuleType type) {
    return type == ModuleType.BE ? beRegistries : uiRegistries;
  }

  public List<ModuleRegistry> getFallbackRegistries(ModuleType type) {
    return type == ModuleType.BE ? beFallbackRegistries : uiFallbackRegistries;
  }
}
