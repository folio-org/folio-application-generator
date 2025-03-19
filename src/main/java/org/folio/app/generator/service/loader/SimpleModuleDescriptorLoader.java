package org.folio.app.generator.service.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.conditions.SimpleCondition;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.registry.SimpleModuleRegistry;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.utils.JsonConverter;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Conditional(SimpleCondition.class)
public class SimpleModuleDescriptorLoader implements ModuleDescriptorLoader {

  private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(504);
  private static final int RETRYABLE_ATTEMPTS_NUMBER = 5;

  private final Log log;
  private final HttpClient httpClient;
  private final JsonConverter jsonConverter;

  @Override
  public Optional<Map<String, Object>> findModuleDescriptor(ModuleRegistry registry, ModuleDefinition module) {
    var simpleRegistry = (SimpleModuleRegistry) registry;
    var url = simpleRegistry.getUrl();
    try {
      return loadModuleDescriptor(url, module);
    } catch (Exception e) {
      HttpRequest request = prepareHttpRequest(url, module);
      log.warn(String.format("Failed to load module descriptor '%s' from %s", module.getId(),
          request.uri().toString()), e);
      return Optional.empty();
    }
  }

  @Override
  public RegistryType getType() {
    return RegistryType.SIMPLE;
  }

  private Optional<Map<String, Object>> loadModuleDescriptor(String url, ModuleDefinition module) throws Exception {
    HttpRequest request = prepareHttpRequest(url, module);
    var moduleId = module.getId();

    var response = retryLoad(request);
    var responseStatus = response.statusCode();

    if (responseStatus != 200) {
      log.warn(String.format("Failed to load module descriptor '%s' from %s: %s", moduleId,
          request.uri().toString(), responseStatus));
      return Optional.empty();
    }

    var searchResult = jsonConverter.parse(response.body(), new TypeReference<Map<String, Object>>() {});
    if (searchResult.isEmpty()) {
      log.warn(String.format("Module descriptor '%s' is not found in %s", moduleId, request.uri().toString()));
      return Optional.empty();
    }

    log.info(String.format("Module descriptor '%s' loaded from %s", moduleId, request.uri().toString()));
    return Optional.of(searchResult);
  }

  private HttpResponse<InputStream> retryLoad(HttpRequest request) throws IOException, InterruptedException {
    var attemptsCount = 0;
    var response = httpClient.send(request, BodyHandlers.ofInputStream());
    while (RETRYABLE_STATUS_CODES.contains(response.statusCode()) && attemptsCount++ < RETRYABLE_ATTEMPTS_NUMBER) {
      response = httpClient.send(request, BodyHandlers.ofInputStream());
    }

    return response;
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

    return baseUrl + "/" + moduleName + "-" + version;
  }
}
