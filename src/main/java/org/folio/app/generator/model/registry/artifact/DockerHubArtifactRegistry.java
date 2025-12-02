package org.folio.app.generator.model.registry.artifact;

import lombok.EqualsAndHashCode;
import org.folio.app.generator.model.types.ArtifactRegistryType;

@EqualsAndHashCode(callSuper = true)
public class DockerHubArtifactRegistry extends AbstractArtifactRegistry<DockerHubArtifactRegistry> {

  public static final String DEFAULT_BASE_URL = "https://hub.docker.com/v2/repositories";

  private static final ArtifactRegistryType TYPE = ArtifactRegistryType.DOCKER_HUB;

  public DockerHubArtifactRegistry() {
    super(DEFAULT_BASE_URL);
  }

  @Override
  public ArtifactRegistryType getType() {
    return TYPE;
  }
}
