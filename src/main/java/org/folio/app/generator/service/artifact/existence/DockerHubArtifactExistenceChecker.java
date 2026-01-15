package org.folio.app.generator.service.artifact.existence;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistry;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.utils.JsonConverter;
import org.springframework.stereotype.Component;

@Component
public class DockerHubArtifactExistenceChecker extends HttpArtifactExistenceChecker {

  private static final int SUCCESS_STATUS_CODE = 200;
  private static final int NOT_FOUND_STATUS_CODE = 404;
  private static final int SERVER_ERROR_STATUS_CODE = 500;

  public DockerHubArtifactExistenceChecker(HttpClient httpClient, Log log, JsonConverter jsonConverter) {
    super(httpClient, log, jsonConverter);
  }

  @Override
  public ModuleType getModuleType() {
    return ModuleType.BE;
  }

  @Override
  @SneakyThrows
  public boolean exists(ModuleDefinition module, ArtifactRegistry registry) {
    var url = buildUrl(registry.getBaseUrl(), registry.getNamespace(), module.getName(), module.getVersion());
    log.debug("Checking Docker image existence: " + url);

    var request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofMinutes(1))
      .GET()
      .build();

    var response = retryLoad(request, BodyHandlers.discarding());
    var statusCode = response.statusCode();

    if (statusCode == SUCCESS_STATUS_CODE) {
      log.debug("Docker image found: " + module.getName() + ":" + module.getVersion());
      return true;
    }

    if (statusCode == NOT_FOUND_STATUS_CODE) {
      log.warn("Docker image not found: " + module.getName() + ":" + module.getVersion()
        + " (status: " + statusCode + ", url: " + url + ")");
      return false;
    }

    if (statusCode >= SERVER_ERROR_STATUS_CODE) {
      throw new ApplicationGeneratorException(
        String.format("Docker Hub server error %d for %s:%s (url: %s)",
          statusCode, module.getName(), module.getVersion(), url),
        ErrorCategory.INFRASTRUCTURE);
    }

    throw new ApplicationGeneratorException(
      String.format("Docker Hub access error %d for %s:%s (url: %s)",
        statusCode, module.getName(), module.getVersion(), url),
      ErrorCategory.INFRASTRUCTURE);
  }

  private static String buildUrl(String baseUrl, String namespace, String imageName, String version) {
    return cleanUrl(baseUrl) + "/" + namespace + "/" + imageName + "/tags/" + version;
  }
}
