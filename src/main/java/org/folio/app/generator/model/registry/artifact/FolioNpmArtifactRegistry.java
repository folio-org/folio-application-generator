package org.folio.app.generator.model.registry.artifact;

import lombok.EqualsAndHashCode;
import org.folio.app.generator.model.types.ArtifactRegistryType;

@EqualsAndHashCode(callSuper = true)
public class FolioNpmArtifactRegistry extends AbstractArtifactRegistry<FolioNpmArtifactRegistry> {

  public static final String DEFAULT_BASE_URL = "https://repository.folio.org/repository";

  private static final ArtifactRegistryType TYPE = ArtifactRegistryType.FOLIO_NPM;

  public FolioNpmArtifactRegistry() {
    super(DEFAULT_BASE_URL);
  }

  @Override
  public ArtifactRegistryType getType() {
    return TYPE;
  }
}
