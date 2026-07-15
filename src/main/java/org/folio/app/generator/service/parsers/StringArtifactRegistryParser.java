package org.folio.app.generator.service.parsers;

import static org.apache.commons.lang3.StringUtils.removeEnd;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.DockerHubArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.EcrArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.FolioNpmArtifactRegistry;
import org.springframework.stereotype.Component;

@Component
public class StringArtifactRegistryParser {

  private static final String PATH_DELIMITER = "/";

  private final Pattern dockerHubPattern1 = Pattern.compile("(docker-hub)::(.{1,1024})::(.{1,1024})");
  private final Pattern dockerHubPattern2 = Pattern.compile("(docker-hub)::(.{1,1024})");
  private final Pattern folioNpmPattern1 = Pattern.compile("(folio-npm)::(.{1,1024})::(.{1,1024})");
  private final Pattern folioNpmPattern2 = Pattern.compile("(folio-npm)::(.{1,1024})");
  private final Pattern ecrPattern1 = Pattern.compile("(aws-ecr)::(.{1,1024})::(.{1,1024})");
  private final Pattern ecrPattern2 = Pattern.compile("(aws-ecr)::(.{1,1024})");

  private final List<Pair<Pattern, Function<String[], ArtifactRegistry>>> patterns = List.of(
    Pair.of(dockerHubPattern1, StringArtifactRegistryParser::parseDockerHubString),
    Pair.of(dockerHubPattern2, StringArtifactRegistryParser::parseDockerHubString),
    Pair.of(folioNpmPattern1, StringArtifactRegistryParser::parseFolioNpmString),
    Pair.of(folioNpmPattern2, StringArtifactRegistryParser::parseFolioNpmString),
    Pair.of(ecrPattern1, StringArtifactRegistryParser::parseEcrString),
    Pair.of(ecrPattern2, StringArtifactRegistryParser::parseEcrString));

  /**
   * Parses an artifact registry string into an {@link ArtifactRegistry}.
   *
   * <p>Supported formats:
   * <ul>
   *   <li>{@code docker-hub::<namespace>}</li>
   *   <li>{@code docker-hub::<baseUrl>::<namespace>}</li>
   *   <li>{@code folio-npm::<namespace>}</li>
   *   <li>{@code folio-npm::<baseUrl>::<namespace>}</li>
   *   <li>{@code aws-ecr::<baseUrl>}</li>
   *   <li>{@code aws-ecr::<baseUrl>::<namespace>}</li>
   * </ul>
   *
   * @param registryString the string to parse
   * @return parsed {@link ArtifactRegistry}, empty if the string does not match any supported format
   */
  public Optional<ArtifactRegistry> parse(String registryString) {
    if (StringUtils.isBlank(registryString)) {
      return Optional.empty();
    }

    var value = registryString.trim();
    for (var patternPair : patterns) {
      var pattern = patternPair.getLeft();
      var matcher = pattern.matcher(value);
      if (matcher.matches()) {
        var stringParts = convertToStringPartsArray(matcher);
        return Optional.of(patternPair.getRight().apply(stringParts));
      }
    }

    return Optional.empty();
  }

  private static String[] convertToStringPartsArray(Matcher matcher) {
    var groupCount = matcher.groupCount();
    var parts = new String[groupCount];
    for (int i = 1; i <= groupCount; i++) {
      parts[i - 1] = matcher.group(i);
    }
    return parts;
  }

  private static ArtifactRegistry parseDockerHubString(String[] parts) {
    var registry = new DockerHubArtifactRegistry();
    if (parts.length == 3) {
      var baseUrl = checkAndGetUrl(parts[1]);
      registry.baseUrl(removeEnd(baseUrl.toString(), PATH_DELIMITER));
      registry.namespace(parts[2].trim());
    } else {
      registry.namespace(parts[1].trim());
    }
    return registry;
  }

  private static ArtifactRegistry parseFolioNpmString(String[] parts) {
    var registry = new FolioNpmArtifactRegistry();
    if (parts.length == 3) {
      var baseUrl = checkAndGetUrl(parts[1]);
      registry.baseUrl(removeEnd(baseUrl.toString(), PATH_DELIMITER));
      registry.namespace(parts[2].trim());
    } else {
      registry.namespace(parts[1].trim());
    }
    return registry;
  }

  private static ArtifactRegistry parseEcrString(String[] parts) {
    var registry = new EcrArtifactRegistry();
    var baseUrl = checkAndGetUrl(parts[1]);
    registry.baseUrl(removeEnd(baseUrl.toString(), PATH_DELIMITER));
    if (parts.length == 3) {
      registry.namespace(parts[2].trim());
    }
    return registry;
  }

  private static URL checkAndGetUrl(String probablyUrl) {
    try {
      return new URL(removeEnd(probablyUrl, PATH_DELIMITER));
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid url provided: " + probablyUrl, e);
    }
  }
}
