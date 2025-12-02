package org.folio.app.generator.model.registry.artifact;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.folio.app.generator.model.types.ArtifactRegistryType;

@Getter
@EqualsAndHashCode(callSuper = true)
public class DockerHubArtifactRegistry extends AbstractArtifactRegistry<DockerHubArtifactRegistry> {

  public static final String DEFAULT_BASE_URL = "https://hub.docker.com/v2/repositories";

  private final ArtifactRegistryType type = ArtifactRegistryType.DOCKER_HUB;

  public DockerHubArtifactRegistry() {
    super(DEFAULT_BASE_URL);
  }
}
