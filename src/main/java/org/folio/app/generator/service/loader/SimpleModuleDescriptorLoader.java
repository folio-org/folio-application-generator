package org.folio.app.generator.service.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
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
@Conditional(SimpleCondition.class)
public class SimpleModuleDescriptorLoader extends HttpModuleDescriptorLoader {

  public SimpleModuleDescriptorLoader(HttpClient httpClient, Log log, JsonConverter jsonConverter) {
    super(httpClient, log, jsonConverter);
  }

  @Override
  public Optional<LoaderResultContainer> findModuleDescriptor(ModuleRegistry registry,
    ModuleDefinition module) {
    var simpleRegistry = (SimpleModuleRegistry) registry;
    var request = prepareHttpRequest(simpleRegistry.getUrl(), module);

    try {
      return loadModuleDescriptor(request, module).map(
        md -> new LoaderResultContainer()
          .sourceUrl(createDirectUrl(simpleRegistry.getUrl(), String.valueOf(md.get("id"))))
          .moduleDescriptor(md));
    } catch (Exception e) {
      log.warn(String.format("Failed to load module descriptor '%s' from %s", module.getId(),
        request.uri().toString()), e);
      return Optional.empty();
    }
  }

  @Override
  public RegistryType getType() {
    return RegistryType.SIMPLE;
  }

  private Optional<Map<String, Object>> loadModuleDescriptor(HttpRequest request, ModuleDefinition module)
      throws Exception {

    var uri = request.uri().toString();
    var moduleId = module.getId();

    var response = retryLoad(request);
    var responseStatus = response.statusCode();

    if (responseStatus == 200) {
      var searchResult = jsonConverter.parse(response.body(), new TypeReference<Map<String, Object>>() {});

      if (!searchResult.isEmpty()) {
        log.info(String.format("Module descriptor '%s' loaded from %s", moduleId, uri));
        return Optional.of(searchResult);
      }

      log.warn(String.format("Module descriptor '%s' is not found in %s", moduleId, uri));
    } else {
      log.warn(String.format("Failed to load module descriptor '%s' from %s: %s", moduleId, uri, responseStatus));
    }

    return Optional.empty();
  }

  @SneakyThrows
  private static URL createDirectUrl(String baseUrl, String moduleId) {
    var cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    return new URL(cleanBaseUrl + "/" + moduleId);
  }

  private static HttpRequest prepareHttpRequest(String url, ModuleDefinition module) {
    return HttpRequest.newBuilder()
      .GET()
      .uri(URI.create(prepareUriString(url, module)))
      .timeout(Duration.ofMinutes(5))
      .version(Version.HTTP_1_1)
      .build();
  }

  private static String prepareUriString(String url, ModuleDefinition module) {
    var baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    return baseUrl + "/" + module.getName() + "-" + module.getVersion();
  }
}
