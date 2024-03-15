package org.folio.app.generator.model.registry;

import java.util.List;
import org.folio.app.generator.model.types.ModuleType;

public record ModuleRegistries(List<ModuleRegistry> beRegistries, List<ModuleRegistry> uiRegistries) {

  public List<ModuleRegistry> getRegistries(ModuleType type) {
    return type == ModuleType.BE ? beRegistries : uiRegistries;
  }
}
