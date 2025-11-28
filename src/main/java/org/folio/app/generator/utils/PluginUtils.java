package org.folio.app.generator.utils;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.PreReleaseFilter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginUtils {

  public static final String PATH_DELIMITER = "/";

  /**
   * Creates a {@link ModuleDefinition} object from the given module id to be split.
   *
   * @param moduleId - module id to be split
   * @return {@link Optional} with {@link ModuleDefinition} object if module id split successfully, empty - otherwise
   */
  public static Optional<ModuleDefinition> createModuleDefinitionFromId(String moduleId) {
    if (StringUtils.isBlank(moduleId)) {
      return Optional.empty();
    }

    return splitModuleId(moduleId).map(dependency ->
      new ModuleDefinition().id(moduleId).name(dependency.getName()).version(dependency.getVersion()));
  }

  /**
   * Splits module id to the module name and module version.
   *
   * @param moduleId - module id to be split
   * @return {@link Optional} with {@link Dependency} object if split successfully, empty - otherwise
   */
  public static Optional<Dependency> splitModuleId(String moduleId) {
    for (int i = 0; i < moduleId.length() - 1; i++) {
      if (moduleId.charAt(i) == '-' && Character.isDigit(moduleId.charAt(i + 1))) {
        var name = moduleId.substring(0, i);
        var version = moduleId.substring(i + 1);

        var semver = SemverUtils.parse(version);
        var preRelease = semver != null ? PreReleaseFilter.fromVersion(version) : PreReleaseFilter.TRUE;
        return Optional.of(Dependency.builder().name(name).version(version).preRelease(preRelease).build());
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

  /**
   * Returns empty unmodifiable list if provided value is null.
   *
   * @param value - {@link List} value to check
   * @param <T> - generic type for list element
   * @return {@link Collections#emptyList()} if provided value is null
   */
  public static <T> List<T> emptyIfNull(List<T> value) {
    return value == null ? emptyList() : value;
  }

  /**
   * Checks if collection is null or empty.
   *
   * @param collection - {@link Collection} object to check
   * @return true if collection is null or empty, false - otherwise
   */
  public static boolean isEmpty(Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }
}
