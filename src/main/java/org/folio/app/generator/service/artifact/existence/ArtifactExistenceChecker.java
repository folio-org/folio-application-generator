package org.folio.app.generator.service.artifact.existence;

import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistry;
import org.folio.app.generator.model.types.ArtifactRegistryType;

public interface ArtifactExistenceChecker {

  ArtifactRegistryType getRegistryType();

  boolean exists(ModuleDefinition module, ArtifactRegistry registry);
}
