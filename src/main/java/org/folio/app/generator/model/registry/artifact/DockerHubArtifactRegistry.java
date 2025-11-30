package org.folio.app.generator.model.registry.artifact;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.net.MalformedURLException;
import java.net.URL;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.folio.app.generator.model.types.ArtifactRegistryType;

@Data
public class DockerHubArtifactRegistry implements ArtifactRegistry {

  public static final String DEFAULT_BASE_URL = "https://hub.docker.com/v2/repositories";

  @Setter(AccessLevel.NONE)
  private ArtifactRegistryType type = ArtifactRegistryType.DOCKER_HUB;

  private String baseUrl = DEFAULT_BASE_URL;
  private String namespace;

  public DockerHubArtifactRegistry baseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
    return this;
  }

  public DockerHubArtifactRegistry namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  @Override
  public boolean isValid() {
    if (isBlank(namespace)) {
      return false;
    }

    if (isBlank(baseUrl)) {
      return false;
    }

    try {
      new URL(baseUrl);
      return true;
    } catch (MalformedURLException e) {
      return false;
    }
  }
}
