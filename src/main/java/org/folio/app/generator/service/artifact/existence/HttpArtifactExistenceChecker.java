package org.folio.app.generator.service.artifact.existence;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.utils.JsonConverter;

@RequiredArgsConstructor
public abstract class HttpArtifactExistenceChecker implements ArtifactExistenceChecker {

  protected static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 502, 503, 504);
  protected static final int RETRYABLE_ATTEMPTS_NUMBER = 5;
  protected static final long RETRY_DELAY_MS = 1000;

  protected final HttpClient httpClient;
  protected final Log log;
  protected final JsonConverter jsonConverter;

  protected <T> HttpResponse<T> retryLoad(HttpRequest request, BodyHandler<T> bodyHandler)
      throws IOException, InterruptedException {
    var attemptsCount = 0;
    Exception lastException = null;
    HttpResponse<T> lastResponse = null;

    while (attemptsCount <= RETRYABLE_ATTEMPTS_NUMBER) {
      try {
        var response = httpClient.send(request, bodyHandler);
        if (!RETRYABLE_STATUS_CODES.contains(response.statusCode())) {
          return response;
        }
        lastResponse = response;
        log.debug("Retrying request due to status code " + response.statusCode()
          + " (attempt " + (attemptsCount + 1) + ")");
      } catch (IOException | InterruptedException e) {
        lastException = e;
        log.debug("Retrying request due to exception: " + e.getMessage()
          + " (attempt " + (attemptsCount + 1) + ")");
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
      }

      attemptsCount++;
      if (attemptsCount <= RETRYABLE_ATTEMPTS_NUMBER) {
        Thread.sleep(RETRY_DELAY_MS);
      }
    }

    if (lastResponse != null) {
      return lastResponse;
    }
    if (lastException instanceof IOException ioException) {
      throw ioException;
    }
    if (lastException instanceof InterruptedException interruptedException) {
      throw interruptedException;
    }
    throw new IOException("Max retry attempts reached for request: " + request.uri());
  }

  protected static String cleanUrl(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
