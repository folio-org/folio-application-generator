package org.folio.app.generator.service.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.conditions.SimpleCondition;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.PreReleaseFilter;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.registry.SimpleModuleRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.utils.JsonConverter;
import org.folio.app.generator.utils.PluginUtils;
import org.folio.app.generator.utils.SemverUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(SimpleCondition.class)
@RequiredArgsConstructor
public class SimpleModuleVersionResolver implements ModuleVersionResolver {

  private final HttpClient httpClient;
  private final Log log;
  private final JsonConverter jsonConverter;

  @Override
  public Optional<List<String>> getAvailableVersions(ModuleRegistry registry, Dependency dependency, ModuleType type) {
    var simpleRegistry = (SimpleModuleRegistry) registry;
    var moduleName = dependency.getName();
    var preRelease = dependency.getPreRelease();

    try {
      var request = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create(cleanUrl(simpleRegistry.getUrl())))
        .timeout(Duration.ofMinutes(5))
        .version(Version.HTTP_1_1)
        .build();

      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

      if (response.statusCode() != 200) {
        log.warn(String.format("Failed to fetch versions for module '%s' from Simple registry: HTTP %d",
          moduleName, response.statusCode()));
        return Optional.empty();
      }

      var modules = jsonConverter.parse(response.body(), new TypeReference<List<Map<String, Object>>>() {});

      var versions = modules.stream()
        .map(md -> (String) md.get("id"))
        .map(PluginUtils::splitModuleId)
        .flatMap(Optional::stream)
        .filter(dep -> moduleName.equals(dep.getName()))
        .map(Dependency::getVersion)
        .filter(version -> matchesPreReleaseFilter(version, preRelease))
        .sorted(Comparator.comparing((String v) -> SemverUtils.parse(v)).reversed())
        .toList();

      if (versions.isEmpty()) {
        log.warn(String.format("Module '%s' is not found in Simple registry", moduleName));
        return Optional.empty();
      }

      log.info(String.format("Found %d versions for module '%s' in Simple registry", versions.size(), moduleName));
      return Optional.of(versions);
    } catch (Exception e) {
      log.warn(String.format("Failed to fetch versions for module '%s' from Simple registry", moduleName), e);
      return Optional.empty();
    }
  }

  @Override
  public RegistryType getType() {
    return RegistryType.SIMPLE;
  }

  private boolean matchesPreReleaseFilter(String version, PreReleaseFilter filter) {
    var effective = filter == null ? PreReleaseFilter.FALSE : filter;
    var semver = SemverUtils.parse(version);
    var isPreRelease = semver.getPreRelease() != null && !semver.getPreRelease().isEmpty();

    return switch (effective) {
      case TRUE -> true;
      case ONLY -> isPreRelease;
      case FALSE -> !isPreRelease;
    };
  }

  private static String cleanUrl(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
