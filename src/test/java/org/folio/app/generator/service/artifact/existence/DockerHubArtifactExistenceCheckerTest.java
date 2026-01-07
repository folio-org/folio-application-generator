package org.folio.app.generator.service.artifact.existence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.DockerHubArtifactRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.JsonConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DockerHubArtifactExistenceCheckerTest {

  @Mock private HttpClient httpClient;
  @Mock private Log log;
  @Mock private JsonConverter jsonConverter;
  @Mock private HttpResponse<InputStream> httpResponse;

  private DockerHubArtifactExistenceChecker checker;

  @BeforeEach
  void setUp() {
    checker = new DockerHubArtifactExistenceChecker(httpClient, log, jsonConverter);
  }

  @Test
  void getModuleType_positive() {
    assertThat(checker.getModuleType()).isEqualTo(ModuleType.BE);
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_positive_imageFound() throws Exception {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);

    var result = checker.exists(module, registry);

    assertThat(result).isTrue();
    verify(log).debug("Docker image found: mod-users:1.0.0");
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_negative_imageNotFound() throws Exception {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(404);

    var result = checker.exists(module, registry);

    assertThat(result).isFalse();
    verify(log).warn("Docker image not found: mod-users:1.0.0 (status: 404, url: https://hub.docker.com/v2/repositories/folioorg/mod-users/tags/1.0.0)");
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_positive_customBaseUrl() throws Exception {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry()
      .baseUrl("https://custom-registry.io/v2/")
      .namespace("myorg");

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);

    var result = checker.exists(module, registry);

    assertThat(result).isTrue();
    verify(log).debug("Checking Docker image existence: https://custom-registry.io/v2/myorg/mod-users/tags/1.0.0");
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_positive_retryOnRateLimitThenSuccess() throws Exception {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    HttpResponse<InputStream> rateLimitResponse = org.mockito.Mockito.mock(HttpResponse.class);
    HttpResponse<InputStream> successResponse = org.mockito.Mockito.mock(HttpResponse.class);

    when(rateLimitResponse.statusCode()).thenReturn(429);
    when(successResponse.statusCode()).thenReturn(200);

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenReturn(rateLimitResponse)
      .thenReturn(successResponse);

    var result = checker.exists(module, registry);

    assertThat(result).isTrue();
    verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    verify(log).debug("Retrying request due to status code 429 (attempt 1)");
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_positive_retryOnServiceUnavailable() throws Exception {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    HttpResponse<InputStream> unavailableResponse1 = org.mockito.Mockito.mock(HttpResponse.class);
    HttpResponse<InputStream> unavailableResponse2 = org.mockito.Mockito.mock(HttpResponse.class);
    HttpResponse<InputStream> successResponse = org.mockito.Mockito.mock(HttpResponse.class);

    when(unavailableResponse1.statusCode()).thenReturn(503);
    when(unavailableResponse2.statusCode()).thenReturn(503);
    when(successResponse.statusCode()).thenReturn(200);

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenReturn(unavailableResponse1)
      .thenReturn(unavailableResponse2)
      .thenReturn(successResponse);

    var result = checker.exists(module, registry);

    assertThat(result).isTrue();
    verify(httpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_negative_retryLimitExhausted() throws Exception {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    HttpResponse<InputStream> rateLimitResponse = org.mockito.Mockito.mock(HttpResponse.class);
    when(rateLimitResponse.statusCode()).thenReturn(429);

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenReturn(rateLimitResponse);

    var result = checker.exists(module, registry);

    assertThat(result).isFalse();
    verify(httpClient, times(6)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_negative_httpClientThrowsException() throws Exception {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenThrow(new IOException("Connection refused"));

    assertThatThrownBy(() -> checker.exists(module, registry))
      .isInstanceOf(IOException.class)
      .hasMessage("Connection refused");
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_positive_retryOnSocketExceptionThenSuccess() throws Exception {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    HttpResponse<InputStream> successResponse = org.mockito.Mockito.mock(HttpResponse.class);
    when(successResponse.statusCode()).thenReturn(200);

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenThrow(new SocketException("Connection reset"))
      .thenReturn(successResponse);

    var result = checker.exists(module, registry);

    assertThat(result).isTrue();
    verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    verify(log).warn("Network error, retrying (attempt 1): Connection reset");
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_positive_retryOnSocketTimeoutExceptionThenSuccess() throws Exception {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    HttpResponse<InputStream> successResponse = org.mockito.Mockito.mock(HttpResponse.class);
    when(successResponse.statusCode()).thenReturn(200);

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenThrow(new SocketTimeoutException("Read timed out"))
      .thenReturn(successResponse);

    var result = checker.exists(module, registry);

    assertThat(result).isTrue();
    verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    verify(log).warn("Network error, retrying (attempt 1): Read timed out");
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_negative_socketExceptionRetryLimitExhausted() throws Exception {
    var module = new ModuleDefinition().name("mod-users").version("1.0.0");
    var registry = new DockerHubArtifactRegistry().namespace("folioorg");

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenThrow(new SocketException("Connection reset"));

    assertThatThrownBy(() -> checker.exists(module, registry))
      .isInstanceOf(SocketException.class)
      .hasMessage("Connection reset");

    verify(httpClient, times(5)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }
}
