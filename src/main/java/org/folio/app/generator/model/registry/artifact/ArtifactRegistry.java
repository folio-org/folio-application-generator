package org.folio.app.generator.model.registry.artifact;

import org.folio.app.generator.model.types.ArtifactRegistryType;

public interface ArtifactRegistry {

  ArtifactRegistryType getType();

  String getBaseUrl();

  String getNamespace();

  boolean isValid();
}
