package org.folio.app.generator.utils;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.folio.app.generator.model.ModuleDefinition;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginUtils {

  public static Optional<ModuleDefinition> createModuleDefinitionFromId(String moduleId) {
    for (int i = 0; i < moduleId.length() - 1; i++) {
      if (moduleId.charAt(i) == '-' && Character.isDigit(moduleId.charAt(i + 1))) {
        return Optional.of(new ModuleDefinition()
          .id(moduleId)
          .name(moduleId.substring(0, i))
          .version(moduleId.substring(i + 1)));
      }
    }

    return Optional.empty();
  }
}
