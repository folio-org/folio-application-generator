package org.folio.app.generator.service.parsers;

import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.DockerHubArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.FolioNpmArtifactRegistry;
import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.springframework.stereotype.Component;

@Component
public class StringArtifactRegistryParser {

  private static final String PATH_DELIMITER = "/";
  private static final String URL_NAMESPACE_DELIMITER = "::";

  public Optional<ArtifactRegistry> parseDocker(String registryString) {
    if (StringUtils.isBlank(registryString)) {
      return Optional.empty();
    }

    var value = registryString.trim();
    var delimiterIndex = value.indexOf(URL_NAMESPACE_DELIMITER);

    if (delimiterIndex > 0) {
      var baseUrl = checkAndGetUrl(value.substring(0, delimiterIndex));
      var namespace = value.substring(delimiterIndex + URL_NAMESPACE_DELIMITER.length()).trim();
      return Optional.of(new DockerHubArtifactRegistry()
        .baseUrl(removeEnd(baseUrl.toString(), PATH_DELIMITER))
        .namespace(namespace));
    }

    return Optional.of(new DockerHubArtifactRegistry().namespace(value));
  }

  public Optional<ArtifactRegistry> parseNpm(String registryString) {
    if (StringUtils.isBlank(registryString)) {
      return Optional.empty();
    }

    var value = registryString.trim();
    var delimiterIndex = value.indexOf(URL_NAMESPACE_DELIMITER);

    if (delimiterIndex > 0) {
      var baseUrl = checkAndGetUrl(value.substring(0, delimiterIndex));
      var repository = value.substring(delimiterIndex + URL_NAMESPACE_DELIMITER.length()).trim();
      return Optional.of(new FolioNpmArtifactRegistry()
        .baseUrl(removeEnd(baseUrl.toString(), PATH_DELIMITER))
        .namespace(repository));
    }

    return Optional.of(new FolioNpmArtifactRegistry().namespace(value));
  }

  public Optional<ArtifactRegistry> parse(String registryString, ArtifactRegistryType type) {
    return type == ArtifactRegistryType.DOCKER_HUB ? parseDocker(registryString) : parseNpm(registryString);
  }

  private static URL checkAndGetUrl(String probablyUrl) {
    try {
      return new URL(removeEnd(probablyUrl, PATH_DELIMITER));
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid url provided: " + probablyUrl, e);
    }
  }
}
