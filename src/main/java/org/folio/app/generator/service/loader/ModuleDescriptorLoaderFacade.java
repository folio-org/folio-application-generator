package org.folio.app.generator.service.loader;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.types.RegistryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ModuleDescriptorLoaderFacade {

  private final Log log;
  private final Map<RegistryType, ModuleDescriptorLoader> loadersMap;

  @Autowired
  public ModuleDescriptorLoaderFacade(Log log, List<ModuleDescriptorLoader> loaders) {
    this.log = log;
    this.loadersMap = loaders.stream().collect(toMap(ModuleDescriptorLoader::getType, identity()));
  }

  /**
   * Tries to find module descriptor in specified registry using {@link ModuleDefinition} object.
   *
   * @param registry - {@link ModuleRegistry} description
   * @param module - {@link ModuleDefinition} object with required information to find a module
   * @return {@link Optional} of {@link Map} as module descriptor
   */
  public Optional<Map<String, Object>> find(ModuleRegistry registry, ModuleDefinition module) {
    var moduleDescriptorLoader = loadersMap.get(registry.getType());
    if (moduleDescriptorLoader == null) {
      log.warn("Failed to find module descriptor loader for a registry: " + registry.getClass().getSimpleName());
      return Optional.empty();
    }

    return moduleDescriptorLoader.findModuleDescriptor(registry, module);
  }
}
