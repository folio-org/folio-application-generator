package org.folio.app.generator.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.apache.maven.plugin.logging.Log;

@UtilityClass
public class HttpRequestUtils {

  public static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 500, 502, 503, 504);
  public static final int RETRYABLE_ATTEMPTS_NUMBER = 5;
  public static final long RETRY_DELAY_MS = 1000;

  public static HttpResponse<InputStream> sendWithRetry(HttpClient httpClient, Log log, HttpRequest request)
      throws IOException, InterruptedException {
    return sendWithRetry(httpClient, log, request, HttpResponse.BodyHandlers.ofInputStream());
  }

  /**
   * Sends the request, retrying on retryable status codes and on any {@link IOException}
   * (connection reset, SSL handshake failure, timeouts, etc.) with a linear back-off.
   *
   * @param httpClient  - {@link HttpClient} to send the request with
   * @param log         - Maven logger
   * @param request     - {@link HttpRequest} to send
   * @param bodyHandler - {@link BodyHandler} for the response body
   * @return the first response with a non-retryable status code, or the response of the final attempt
   * @throws IOException if the last attempt fails with a network error
   */
  public static <T> HttpResponse<T> sendWithRetry(HttpClient httpClient, Log log, HttpRequest request,
      BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
    var attemptsCount = 0;
    IOException lastException = null;

    while (attemptsCount < RETRYABLE_ATTEMPTS_NUMBER) {
      try {
        var response = httpClient.send(request, bodyHandler);
        var statusCode = response.statusCode();
        if (!RETRYABLE_STATUS_CODES.contains(statusCode)) {
          return response;
        }
        log.debug("Retrying request due to status code " + statusCode
          + " (attempt " + (attemptsCount + 1) + ")");
      } catch (IOException e) {
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
    return httpClient.send(request, bodyHandler);
  }

  public static String cleanUrl(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  /**
   * Adds the provided custom headers to the given request builder.
   *
   * @param builder - {@link HttpRequest.Builder} to enrich with headers
   * @param headers - map of header name to value, may be {@code null} or empty
   */
  public static void applyHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
    if (headers == null || headers.isEmpty()) {
      return;
    }

    headers.forEach(builder::header);
  }
}
