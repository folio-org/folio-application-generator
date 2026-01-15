package org.folio.app.generator.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class HttpRetryHelperTest {

  @Mock
  private HttpClient httpClient;
  @Mock
  private Log log;
  @Mock
  private HttpRequest request;
  @Mock
  private HttpResponse<Object> response;

  @Test
  void sendWithRetry_positive_immediateSuccess() throws IOException, InterruptedException {
    when(httpClient.send(any(), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(200);

    var result = HttpRetryHelper.sendWithRetry(httpClient, log, request);

    assertThat(result).isSameAs(response);
    verify(httpClient, times(1)).send(any(), any());
  }

  @Test
  void sendWithRetry_positive_retryOnStatusCodeThenSuccess() throws IOException, InterruptedException {
    when(httpClient.send(any(), any())).thenReturn(response);
    when(response.statusCode())
      .thenReturn(504)
      .thenReturn(200);

    var result = HttpRetryHelper.sendWithRetry(httpClient, log, request);

    assertThat(result).isSameAs(response);
    verify(httpClient, times(2)).send(any(), any());
    verify(log).debug("Retrying request due to status code 504 (attempt 1)");
  }

  @Test
  void sendWithRetry_positive_retryOnSocketExceptionThenSuccess() throws IOException, InterruptedException {
    when(httpClient.send(any(), any()))
      .thenThrow(new SocketException("Connection reset"))
      .thenReturn(response);
    when(response.statusCode()).thenReturn(200);

    var result = HttpRetryHelper.sendWithRetry(httpClient, log, request);

    assertThat(result).isSameAs(response);
    verify(httpClient, times(2)).send(any(), any());
    verify(log).warn("Network error, retrying (attempt 1): Connection reset");
  }

  @Test
  void sendWithRetry_positive_retryOnSocketTimeoutExceptionThenSuccess() throws IOException, InterruptedException {
    when(httpClient.send(any(), any()))
      .thenThrow(new SocketTimeoutException("Read timed out"))
      .thenReturn(response);
    when(response.statusCode()).thenReturn(200);

    var result = HttpRetryHelper.sendWithRetry(httpClient, log, request);

    assertThat(result).isSameAs(response);
    verify(httpClient, times(2)).send(any(), any());
    verify(log).warn("Network error, retrying (attempt 1): Read timed out");
  }

  @Test
  void sendWithRetry_positive_retryOnHttpTimeoutExceptionThenSuccess() throws IOException, InterruptedException {
    when(httpClient.send(any(), any()))
      .thenThrow(new HttpTimeoutException("request timed out"))
      .thenReturn(response);
    when(response.statusCode()).thenReturn(200);

    var result = HttpRetryHelper.sendWithRetry(httpClient, log, request);

    assertThat(result).isSameAs(response);
    verify(httpClient, times(2)).send(any(), any());
    verify(log).warn("Network error, retrying (attempt 1): request timed out");
  }

  @Test
  void sendWithRetry_positive_retryWithNullExceptionMessage() throws IOException, InterruptedException {
    var socketException = mock(SocketException.class);
    when(socketException.getMessage()).thenReturn(null);
    when(httpClient.send(any(), any()))
      .thenThrow(socketException)
      .thenReturn(response);
    when(response.statusCode()).thenReturn(200);

    var result = HttpRetryHelper.sendWithRetry(httpClient, log, request);

    assertThat(result).isSameAs(response);
    verify(log).warn("Network error, retrying (attempt 1): SocketException");
  }

  @Test
  void sendWithRetry_negative_exhaustRetriesOnNetworkError() throws IOException, InterruptedException {
    var exception = new SocketException("Connection refused");
    when(httpClient.send(any(), any())).thenThrow(exception);

    assertThatThrownBy(() -> HttpRetryHelper.sendWithRetry(httpClient, log, request))
      .isInstanceOf(SocketException.class)
      .hasMessage("Connection refused");

    verify(httpClient, times(5)).send(any(), any());
    verify(log).warn("Network error, retrying (attempt 1): Connection refused");
    verify(log).warn("Network error, retrying (attempt 2): Connection refused");
    verify(log).warn("Network error, retrying (attempt 3): Connection refused");
    verify(log).warn("Network error, retrying (attempt 4): Connection refused");
    verify(log).warn("Network error, retrying (attempt 5): Connection refused");
  }

  @Test
  void sendWithRetry_negative_exhaustRetriesOnStatusCode() throws IOException, InterruptedException {
    when(httpClient.send(any(), any())).thenReturn(response);
    when(response.statusCode())
      .thenReturn(504).thenReturn(504)
      .thenReturn(504).thenReturn(504)
      .thenReturn(504).thenReturn(200);

    var result = HttpRetryHelper.sendWithRetry(httpClient, log, request);

    assertThat(result).isSameAs(response);
    verify(httpClient, times(6)).send(any(), any());
  }

  @Test
  void sendWithRetry_positive_allRetryableStatusCodes() throws IOException, InterruptedException {
    when(httpClient.send(any(), any())).thenReturn(response);
    when(response.statusCode())
      .thenReturn(429)
      .thenReturn(502)
      .thenReturn(503)
      .thenReturn(504)
      .thenReturn(200);

    var result = HttpRetryHelper.sendWithRetry(httpClient, log, request);

    assertThat(result).isSameAs(response);
    verify(httpClient, times(5)).send(any(), any());
  }

  @Test
  void cleanUrl_removesTrailingSlash() {
    var result = HttpRetryHelper.cleanUrl("http://example.com/");

    assertThat(result).isEqualTo("http://example.com");
  }

  @Test
  void cleanUrl_noChangeWithoutTrailingSlash() {
    var result = HttpRetryHelper.cleanUrl("http://example.com");

    assertThat(result).isEqualTo("http://example.com");
  }

  @Test
  void cleanUrl_handlesMultiplePathSegments() {
    var result = HttpRetryHelper.cleanUrl("http://example.com/api/v1/");

    assertThat(result).isEqualTo("http://example.com/api/v1");
  }

  @Test
  void constants_haveExpectedValues() {
    assertThat(HttpRetryHelper.RETRYABLE_STATUS_CODES).containsExactlyInAnyOrder(429, 500, 502, 503, 504);
    assertThat(HttpRetryHelper.RETRYABLE_ATTEMPTS_NUMBER).isEqualTo(5);
    assertThat(HttpRetryHelper.RETRY_DELAY_MS).isEqualTo(1000);
  }
}
