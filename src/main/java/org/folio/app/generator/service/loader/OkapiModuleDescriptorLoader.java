package org.folio.app.generator.service.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.conditions.OkapiCondition;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.utils.JsonConverter;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Conditional(OkapiCondition.class)
public class OkapiModuleDescriptorLoader implements ModuleDescriptorLoader {

  private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(504);
  private static final int RETRYABLE_ATTEMPTS_NUMBER = 5;

  private final Log log;
  private final HttpClient httpClient;
  private final JsonConverter jsonConverter;

  @Override
  public Optional<LoaderResultContainer> findModuleDescriptor(ModuleRegistry registry,
    ModuleDefinition module) {
    var okapiRegistry = (OkapiModuleRegistry) registry;
    var url = okapiRegistry.getUrl();
    try {
      return loadModuleDescriptor(url, module).map(
        md -> new LoaderResultContainer()
          .sourceUrl(createDirectUrl(url, String.valueOf(md.get("id"))))
          .moduleDescriptor(md));
    } catch (Exception e) {
      log.warn(String.format("Failed to load module descriptor '%s' from %s", module.getId(), url), e);
      return Optional.empty();
    }
  }

  @Override
  public RegistryType getType() {
    return RegistryType.OKAPI;
  }

  @SneakyThrows
  private Optional<Map<String, Object>> loadModuleDescriptor(String url, ModuleDefinition module) {
    var request = prepareHttpRequest(url, module);
    var moduleId = module.getId();

    var response = retryLoad(request);
    var responseStatus = response.statusCode();
    if (responseStatus != 200) {
      log.warn(String.format("Failed to load module descriptor '%s' from %s: %s", moduleId, url, responseStatus));
      return Optional.empty();
    }

    var searchResult = jsonConverter.parse(response.body(), new TypeReference<List<Map<String, Object>>>() {
    });
    if (searchResult.isEmpty()) {
      log.warn(String.format("Module descriptor '%s' is not found in %s", moduleId, url));
      return Optional.empty();
    }

    log.info(String.format("Module descriptor '%s' loaded from %s", moduleId, url));
    return Optional.of(searchResult.get(0));
  }

  private HttpResponse<InputStream> retryLoad(HttpRequest request) throws IOException, InterruptedException {
    var attemptsCount = 0;
    var response = httpClient.send(request, BodyHandlers.ofInputStream());
    while (RETRYABLE_STATUS_CODES.contains(response.statusCode()) && attemptsCount++ < RETRYABLE_ATTEMPTS_NUMBER) {
      response = httpClient.send(request, BodyHandlers.ofInputStream());
    }

    return response;
  }

  @SneakyThrows
  private static URL createDirectUrl(String baseUrl, String moduleId) {
    var cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    return new URL(cleanBaseUrl + "/_/proxy/modules/" + moduleId);
  }

  private static HttpRequest prepareHttpRequest(String url, ModuleDefinition module) {
    var baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    return HttpRequest.newBuilder()
      .GET()
      .uri(URI.create(prepareUriString(baseUrl, module)))
      .timeout(Duration.ofMinutes(5))
      .version(Version.HTTP_1_1)
      .build();
  }

  private static String prepareUriString(String baseUrl, ModuleDefinition module) {
    var moduleName = module.getName();
    var version = module.getVersion();
    var filter = "latest".equals(version) ? moduleName : module.getId();

    return baseUrl + "/_/proxy/modules"
      + "?filter=" + filter
      + "&latest=1"
      + "&orderBy=id"
      + "&order=desc"
      + "&full=true";
  }
}
