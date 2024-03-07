package org.folio.app.generator.service.loader;

import java.util.Map;
import java.util.Optional;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.types.RegistryType;

public interface ModuleDescriptorLoader {

  Optional<Map<String, Object>> findModuleDescriptor(ModuleRegistry registry, ModuleDefinition artifact);

  RegistryType getType();
}
