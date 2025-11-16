package org.folio.app.generator.service.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.conditions.OkapiCondition;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.utils.JsonConverter;
import org.folio.app.generator.utils.PluginUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(OkapiCondition.class)
@RequiredArgsConstructor
public class OkapiModuleVersionResolver implements ModuleVersionResolver {

  private final HttpClient httpClient;
  private final Log log;
  private final JsonConverter jsonConverter;

  @Override
  public Optional<List<String>> getAvailableVersions(ModuleRegistry registry, Dependency module, ModuleType type) {
    var okapiRegistry = (OkapiModuleRegistry) registry;
    var url = okapiRegistry.getUrl();
    try {
      return getVersions(url, module, type);
    } catch (Exception e) {
      log.warn(String.format("Failed to fetch versions for module '%s' from %s", module.getName(), url), e);
      return Optional.empty();
    }
  }

  @Override
  public RegistryType getType() {
    return RegistryType.OKAPI;
  }

  private Optional<List<String>> getVersions(String url, Dependency module, ModuleType type) throws Exception {
    var request = prepareHttpRequest(url, module, type);
    var moduleName = module.getName();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    var responseStatus = response.statusCode();

    if (responseStatus != 200) {
      log.warn(String.format("Failed to fetch versions for module '%s' from %s: HTTP %d",
        moduleName, url, responseStatus));
      return Optional.empty();
    }

    var modules = jsonConverter.parse(response.body(), new TypeReference<List<Map<String, Object>>>() {});
    var versions = modules.stream()
      .map(md -> String.valueOf(md.get("id")))
      .map(PluginUtils::splitModuleId)
      .flatMap(Optional::stream)
      .map(Dependency::getVersion)
      .toList();

    if (versions.isEmpty()) {
      log.warn(String.format("Module '%s' is not found in %s", moduleName, url));
      return Optional.empty();
    }

    log.debug(String.format("Module '%s' versions fetched from %s", moduleName, url));
    return Optional.of(versions);
  }

  private static String cleanUrl(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  private static HttpRequest prepareHttpRequest(String url, Dependency module, ModuleType type) {
    var baseUrl = cleanUrl(url);
    return HttpRequest.newBuilder()
      .GET()
      .uri(URI.create(prepareUriString(baseUrl, module, type)))
      .timeout(Duration.ofMinutes(5))
      .version(Version.HTTP_1_1)
      .build();
  }

  private static String prepareUriString(String baseUrl, Dependency module, ModuleType type) {
    var moduleName = module.getName();
    var preRelease = module.getPreRelease();
    var preReleaseFilter = type == ModuleType.UI ? "&npmSnapshot=" : "&preRelease=";

    return baseUrl + "/_/proxy/modules"
      + "?filter=" + moduleName
      + preReleaseFilter + (preRelease == null ? "false" : preRelease.getValue())
      + "&orderBy=id"
      + "&order=desc";
  }
}
