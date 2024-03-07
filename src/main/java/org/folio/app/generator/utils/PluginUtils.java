package org.folio.app.generator.utils;

import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.app.generator.model.ModuleDefinition;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginUtils {

  public static final String PATH_DELIMITER = "/";

  /**
   * Creates a {@link ModuleDefinition} object from the given module id.
   *
   * <p>This method splits module id to the module name and module version</p>
   *
   * @param moduleId - module id to be parsed
   * @return {@link Optional} with {@link ModuleDefinition} object if module id parsed successfully, empty - otherwise
   */
  public static Optional<ModuleDefinition> createModuleDefinitionFromId(String moduleId) {
    if (StringUtils.isBlank(moduleId)) {
      return Optional.empty();
    }

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

  /**
   * Collects values and represents them as a bulleted list after error.
   *
   * <p>Example of response of joining this value to an error message:</p>
   * <pre>
   *   Invalid values found:
   *     * value1
   *     * value2
   * </pre>
   *
   * @param values - list of values to process
   * @return bulleted list as {@link String} object
   */
  public static String collectToBulletedList(Collection<String> values) {
    if (values == null || values.isEmpty()) {
      return "";
    }

    return values.stream().collect(joining("\n  * ", "\n  * ", ""));
  }
}
