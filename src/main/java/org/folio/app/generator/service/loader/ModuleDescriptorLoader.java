package org.folio.app.generator.service.loader;

import java.util.Optional;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.types.RegistryType;

public interface ModuleDescriptorLoader {

  /**
   * Tries to find module descriptor in specified registry using {@link ModuleDefinition} object.
   *
   * @param registry - {@link ModuleRegistry} description
   * @param artifact - {@link ModuleDefinition} object with required information to find a module
   * @return {@link Optional} of {@link LoaderResultContainer}
   */
  Optional<LoaderResultContainer> findModuleDescriptor(ModuleRegistry registry, ModuleDefinition artifact);

  RegistryType getType();
}
