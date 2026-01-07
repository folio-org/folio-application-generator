package org.folio.app.generator.service.resolver;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.conditions.OkapiCondition;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.ErrorDetail;
import org.folio.app.generator.model.PreReleaseFilter;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.utils.JsonConverter;
import org.folio.app.generator.utils.PluginUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(OkapiCondition.class)
@RequiredArgsConstructor
public class OkapiModuleVersionResolver implements ModuleVersionResolver {

  private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 502, 503, 504);
  private static final int RETRYABLE_ATTEMPTS_NUMBER = 5;
  private static final long RETRY_DELAY_MS = 1000;

  private final HttpClient httpClient;
  private final Log log;
  private final JsonConverter jsonConverter;

  @Override
  public Optional<List<String>> getAvailableVersions(ModuleRegistry registry, Dependency module, ModuleType type) {
    var okapiRegistry = (OkapiModuleRegistry) registry;
    var url = okapiRegistry.getUrl();
    try {
      return getVersions(url, module, type);
    } catch (IOException e) {
      log.warn(String.format("Failed to fetch versions for module '%s' from %s", module.getName(), url), e);
      var errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      var errorDetail = ErrorDetail.infrastructureError(url, errorMsg);
      throw new ApplicationGeneratorException(
        String.format("Network error while fetching versions for module '%s' from %s: %s",
          module.getName(), url, errorMsg),
        ErrorCategory.INFRASTRUCTURE, errorDetail, e);
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

    var response = sendWithRetry(request);
    var responseStatus = response.statusCode();

    if (responseStatus != 200) {
      log.warn(String.format("Failed to fetch versions for module '%s' from %s: HTTP %d",
        moduleName, url, responseStatus));
      return Optional.empty();
    }

    List<Map<String, Object>> modules;
    try (var inputStream = response.body()) {
      modules = jsonConverter.parse(inputStream, new TypeReference<>() {});
    }

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

  private HttpResponse<java.io.InputStream> sendWithRetry(HttpRequest request)
    throws IOException, InterruptedException {
    var attemptsCount = 0;
    IOException lastException = null;

    while (attemptsCount < RETRYABLE_ATTEMPTS_NUMBER) {
      try {
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (!RETRYABLE_STATUS_CODES.contains(response.statusCode())) {
          return response;
        }
        log.debug("Retrying request due to status code " + response.statusCode()
          + " (attempt " + (attemptsCount + 1) + ")");
      } catch (SocketException | SocketTimeoutException | HttpTimeoutException e) {
        lastException = e;
        var errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        log.warn("Network error, retrying (attempt " + (attemptsCount + 1) + "): " + errorMsg);
      }
      attemptsCount++;
      Thread.sleep(RETRY_DELAY_MS * attemptsCount);
    }

    if (lastException != null) {
      throw lastException;
    }
    return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
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
    var preRelease = module.getPreRelease() == null ? PreReleaseFilter.TRUE : module.getPreRelease();
    var preReleaseFilter = type == ModuleType.UI ? "&npmSnapshot=" : "&preRelease=";

    return baseUrl + "/_/proxy/modules"
      + "?filter=" + moduleName
      + preReleaseFilter + preRelease.getValue()
      + "&orderBy=id"
      + "&order=desc";
  }
}
