package org.folio.app.generator.service.artifact.existence;

import com.fasterxml.jackson.core.type.TypeReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.utils.JsonConverter;
import org.springframework.stereotype.Component;

@Component
public class FolioNpmArtifactExistenceChecker extends HttpArtifactExistenceChecker {

  private static final int SUCCESS_STATUS_CODE = 200;

  public FolioNpmArtifactExistenceChecker(HttpClient httpClient, Log log, JsonConverter jsonConverter) {
    super(httpClient, log, jsonConverter);
  }

  @Override
  public ModuleType getModuleType() {
    return ModuleType.UI;
  }

  @Override
  @SneakyThrows
  public boolean exists(ModuleDefinition module, ArtifactRegistry registry) {
    var packageName = transformModuleNameToPackage(module.getName());
    var url = buildUrl(registry.getBaseUrl(), registry.getNamespace(), packageName);
    log.debug("Checking NPM package existence: " + url);

    var request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofMinutes(1))
      .GET()
      .build();

    var response = retryLoad(request, BodyHandlers.ofInputStream());
    var statusCode = response.statusCode();

    if (statusCode != SUCCESS_STATUS_CODE) {
      log.debug("NPM package not found: " + packageName + " (status: " + statusCode + ")");
      return false;
    }

    Map<?, ?> versions;
    try (var inputStream = response.body()) {
      var body = jsonConverter.parse(inputStream, new TypeReference<Map<String, Object>>() {});
      versions = (Map<?, ?>) body.get("versions");
    }

    if (versions == null || !versions.containsKey(module.getVersion())) {
      log.debug("NPM package version not found: " + packageName + "@" + module.getVersion());
      return false;
    }

    log.debug("NPM package found: " + packageName + "@" + module.getVersion());
    return true;
  }

  public static String transformModuleNameToPackage(String moduleName) {
    var name = moduleName;
    if (name.startsWith("folio_")) {
      name = name.substring(6);
    }
    name = name.replace('_', '-');
    return "@folio/" + name;
  }

  private static String buildUrl(String baseUrl, String repository, String packageName) {
    return cleanUrl(baseUrl) + "/" + repository + "/" + packageName;
  }
}
