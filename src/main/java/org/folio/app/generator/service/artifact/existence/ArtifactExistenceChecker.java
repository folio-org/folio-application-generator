package org.folio.app.generator.service.artifact.existence;

import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistry;
import org.folio.app.generator.model.types.ModuleType;

public interface ArtifactExistenceChecker {

  ModuleType getModuleType();

  boolean exists(ModuleDefinition module, ArtifactRegistry registry);
}
