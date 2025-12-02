package org.folio.app.generator.model.registry.artifact;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.folio.app.generator.model.types.ArtifactRegistryType;

@Getter
@EqualsAndHashCode(callSuper = true)
public class FolioNpmArtifactRegistry extends AbstractArtifactRegistry<FolioNpmArtifactRegistry> {

  public static final String DEFAULT_BASE_URL = "https://repository.folio.org/repository";

  private final ArtifactRegistryType type = ArtifactRegistryType.FOLIO_NPM;

  public FolioNpmArtifactRegistry() {
    super(DEFAULT_BASE_URL);
  }
}
