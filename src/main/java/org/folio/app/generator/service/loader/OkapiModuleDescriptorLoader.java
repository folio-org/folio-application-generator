package org.folio.app.generator.service.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
@Conditional(OkapiCondition.class)
public class OkapiModuleDescriptorLoader extends HttpModuleDescriptorLoader {

  public OkapiModuleDescriptorLoader(HttpClient httpClient, Log log, JsonConverter jsonConverter) {
    super(httpClient, log, jsonConverter);
  }

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
      log.warn(String.format("Failed to load module descriptor '%s' from %s", module.getId(), cleanUrl(url)), e);
      return Optional.empty();
    }
  }

  @Override
  public RegistryType getType() {
    return RegistryType.OKAPI;
  }

  private Optional<Map<String, Object>> loadModuleDescriptor(String url, ModuleDefinition module) {
    var request = prepareHttpRequest(url, module);
    var moduleId = module.getId();

    var response = retryLoad(request);
    var responseStatus = response.statusCode();
    if (responseStatus != 200) {
      log.warn(String.format("Failed to load module descriptor '%s' from %s: %s", moduleId, url, responseStatus));
      return Optional.empty();
    }

    var searchResult = jsonConverter
      .parse(response.body(), new TypeReference<List<Map<String, Object>>>() {});

    if (searchResult.isEmpty()) {
      log.warn(String.format("Module descriptor '%s' is not found in %s", moduleId, url));
      return Optional.empty();
    }

    log.info(String.format("Module descriptor '%s' loaded from %s", moduleId, url));
    return Optional.of(searchResult.get(0));
  }

  @SneakyThrows
  private static URL createDirectUrl(String baseUrl, String moduleId) {
    var cleanBaseUrl = cleanUrl(baseUrl);
    return new URL(cleanBaseUrl + "/_/proxy/modules/" + moduleId);
  }

  private static HttpRequest prepareHttpRequest(String url, ModuleDefinition module) {
    var baseUrl = cleanUrl(url);
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
