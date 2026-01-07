package org.folio.app.generator.service.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.utils.JsonConverter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public abstract class HttpModuleDescriptorLoader implements ModuleDescriptorLoader {

  protected static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 502, 503, 504);
  protected static final int RETRYABLE_ATTEMPTS_NUMBER = 5;
  protected static final long RETRY_DELAY_MS = 1000;

  protected final HttpClient httpClient;
  protected final Log log;
  protected final JsonConverter jsonConverter;

  @SneakyThrows
  protected HttpResponse<InputStream> retryLoad(HttpRequest request) {
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
        log.warn("Network error, retrying (attempt " + (attemptsCount + 1) + "): " + e.getMessage());
      }
      attemptsCount++;
      Thread.sleep(RETRY_DELAY_MS * attemptsCount);
    }

    if (lastException != null) {
      throw lastException;
    }
    return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
  }

  protected static String cleanUrl(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
