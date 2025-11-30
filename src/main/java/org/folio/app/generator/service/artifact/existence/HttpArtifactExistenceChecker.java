package org.folio.app.generator.service.artifact.existence;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.utils.JsonConverter;

@RequiredArgsConstructor
public abstract class HttpArtifactExistenceChecker implements ArtifactExistenceChecker {

  protected static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 502, 503, 504);
  protected static final int RETRYABLE_ATTEMPTS_NUMBER = 5;

  protected final HttpClient httpClient;
  protected final Log log;
  protected final JsonConverter jsonConverter;

  @SneakyThrows
  protected HttpResponse<InputStream> retryLoad(HttpRequest request) {
    var attemptsCount = 0;
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    while (RETRYABLE_STATUS_CODES.contains(response.statusCode()) && attemptsCount++ < RETRYABLE_ATTEMPTS_NUMBER) {
      log.debug("Retrying request due to status code " + response.statusCode() + " (attempt " + attemptsCount + ")");
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }
    return response;
  }

  protected static String cleanUrl(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
