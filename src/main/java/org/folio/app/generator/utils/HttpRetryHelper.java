package org.folio.app.generator.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.apache.maven.plugin.logging.Log;

@UtilityClass
public class HttpRetryHelper {

  public static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 502, 503, 504);
  public static final int RETRYABLE_ATTEMPTS_NUMBER = 5;
  public static final long RETRY_DELAY_MS = 1000;

  public static HttpResponse<InputStream> sendWithRetry(HttpClient httpClient, Log log, HttpRequest request)
      throws IOException, InterruptedException {
    var attemptsCount = 0;
    IOException lastException = null;

    while (attemptsCount < RETRYABLE_ATTEMPTS_NUMBER) {
      try {
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        var statusCode = response.statusCode();
        if (!RETRYABLE_STATUS_CODES.contains(statusCode)) {
          return response;
        }
        log.debug("Retrying request due to status code " + statusCode
          + " (attempt " + (attemptsCount + 1) + ")");
      } catch (SocketException | SocketTimeoutException | HttpTimeoutException e) {
        lastException = e;
        var errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        log.warn("Network error, retrying (attempt " + (attemptsCount + 1) + "): " + errorMsg);
      }
      attemptsCount++;
      Thread.sleep(RETRY_DELAY_MS * attemptsCount);
    }

    if (lastException != null) {
      throw lastException;
    }
    return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
  }

  public static String cleanUrl(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
