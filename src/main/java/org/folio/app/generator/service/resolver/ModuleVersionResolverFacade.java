package org.folio.app.generator.service.resolver;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.types.RegistryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ModuleVersionResolverFacade {

  private final Log log;
  private final Map<RegistryType, ModuleVersionResolver> resolversMap;

  @Autowired
  public ModuleVersionResolverFacade(Log log, List<ModuleVersionResolver> resolvers) {
    this.log = log;
    this.resolversMap = resolvers.stream().collect(toMap(ModuleVersionResolver::getType, identity()));
  }

  /**
   * Gets available versions for a module from the specified registry.
   *
   * @param registry the module registry to query
   * @param dependency the dependency specification
   * @return list of available versions sorted in descending order, or empty if not found or on error
   */
  public Optional<List<String>> getAvailableVersions(ModuleRegistry registry, Dependency dependency) {
    var resolver = resolversMap.get(registry.getType());
    if (resolver == null) {
      log.warn("Failed to find module version resolver for registry: " + registry.getClass().getSimpleName());
      return Optional.empty();
    }
    return resolver.getAvailableVersions(registry, dependency);
  }
}
