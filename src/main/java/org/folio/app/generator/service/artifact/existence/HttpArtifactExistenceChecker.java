package org.folio.app.generator.service.artifact.existence;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.utils.HttpRequestUtils;
import org.folio.app.generator.utils.JsonConverter;

@RequiredArgsConstructor
public abstract class HttpArtifactExistenceChecker implements ArtifactExistenceChecker {

  protected final HttpClient httpClient;
  protected final Log log;
  protected final JsonConverter jsonConverter;

  @SneakyThrows
  protected <T> HttpResponse<T> retryLoad(HttpRequest request, BodyHandler<T> bodyHandler) {
    return HttpRequestUtils.sendWithRetry(httpClient, log, request, bodyHandler);
  }

  protected static String cleanUrl(String url) {
    return HttpRequestUtils.cleanUrl(url);
  }
}
