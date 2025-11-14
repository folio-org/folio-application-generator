package org.folio.app.generator.service.resolver;

import java.util.List;
import java.util.Optional;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.types.RegistryType;

public interface ModuleVersionResolver {

  /**
   * Gets all available versions for a module from a specific registry.
   * This method is used for constraint resolution to find versions matching a semver range.
   * Each implementation can optimize based on registry capabilities.
   *
   * @param registry the module registry to query
   * @param dependency the dependency specification (contains module name, version constraint, and preRelease filter)
   * @return list of available version strings, sorted in descending order (highest first).
   *         Returns empty list if module is not found or on error.
   */
  Optional<List<String>> getAvailableVersions(ModuleRegistry registry, Dependency dependency);

  RegistryType getType();
}
