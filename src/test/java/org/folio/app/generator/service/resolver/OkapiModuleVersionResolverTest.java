package org.folio.app.generator.service.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.PreReleaseFilter;
import org.folio.app.generator.model.registry.OkapiModuleRegistry;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.JsonConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

@UnitTest
@ExtendWith(MockitoExtension.class)
class OkapiModuleVersionResolverTest {

  private static final String BASE_URL = "http://localhost";

  @InjectMocks private OkapiModuleVersionResolver resolver;
  @Mock private Log log;
  @Mock private HttpClient httpClient;
  @Mock private HttpResponse<Object> httpResponse;
  @Mock private JsonConverter jsonConverter;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(log, jsonConverter);
  }

  @Test
  void getType_positive() {
    assertThat(resolver.getType()).isEqualTo(RegistryType.OKAPI);
  }

  @Test
  void getAvailableVersions_positive_multipleVersions() throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);

    mockHttpResponse(200, List.of(
      Map.of("id", "mod-foo-1.2.0"),
      Map.of("id", "mod-foo-1.1.0"),
      Map.of("id", "mod-foo-1.0.0")));

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.2.0", "1.1.0", "1.0.0");
    verify(log).debug("Module 'mod-foo' versions fetched from http://localhost");
  }

  @Test
  void getAvailableVersions_positive_singleVersion() throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", null);

    mockHttpResponse(200, List.of(Map.of("id", "mod-foo-1.0.0")));

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.0.0");
    verify(log).debug("Module 'mod-foo' versions fetched from http://localhost");
  }

  @Test
  void getAvailableVersions_positive_preReleaseTrue() throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.TRUE);

    mockHttpResponse(200, List.of(
      Map.of("id", "mod-foo-1.1.0-SNAPSHOT"),
      Map.of("id", "mod-foo-1.0.0")));

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.1.0-SNAPSHOT", "1.0.0");
    verify(log).debug("Module 'mod-foo' versions fetched from http://localhost");
  }

  @Test
  void getAvailableVersions_positive_preReleaseOnly() throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.ONLY);

    mockHttpResponse(200, List.of(Map.of("id", "mod-foo-1.1.0-SNAPSHOT.123")));

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.1.0-SNAPSHOT.123");
    verify(log).debug("Module 'mod-foo' versions fetched from http://localhost");
  }

  @Test
  void getAvailableVersions_positive_uiModuleWithNpmSnapshot() throws IOException, InterruptedException {
    var dependency = new Dependency("folio_app", "^1.0.0", PreReleaseFilter.ONLY);

    mockHttpResponse(200, List.of(Map.of("id", "folio_app-1.0.10010000000123")));

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.UI);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.0.10010000000123");
    verify(log).debug("Module 'folio_app' versions fetched from http://localhost");
  }

  @Test
  void getAvailableVersions_negative_emptyResult() throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);

    mockHttpResponse(200, List.of());

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isEmpty();
    verify(log).warn("Module 'mod-foo' is not found in http://localhost");
  }

  @Test
  void getAvailableVersions_negative_httpError() throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);

    mockHttpResponse(404, null);

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isEmpty();
    verify(log).warn("Failed to fetch versions for module 'mod-foo' from http://localhost: HTTP 404");
  }

  @Test
  void getAvailableVersions_negative_httpServerError() throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);

    mockHttpResponse(500, null);

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isEmpty();
    verify(log).warn("Failed to fetch versions for module 'mod-foo' from http://localhost: HTTP 500");
  }

  @Test
  void getAvailableVersions_negative_ioExceptionThrowsApplicationGeneratorException()
      throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var exception = new IOException("Connection refused");
    var registry = okapiRegistry();

    when(httpClient.send(any(HttpRequest.class), any())).thenThrow(exception);

    assertThatThrownBy(() -> resolver.getAvailableVersions(registry, dependency, ModuleType.BE))
      .isInstanceOf(ApplicationGeneratorException.class)
      .hasMessageContaining("Network error while fetching versions for module 'mod-foo' from http://localhost")
      .hasCause(exception)
      .satisfies(e -> assertThat(((ApplicationGeneratorException) e).getCategory())
        .isEqualTo(ErrorCategory.INFRASTRUCTURE));
    verify(log).warn("Failed to fetch versions for module 'mod-foo' from http://localhost", exception);
  }

  @Test
  void getAvailableVersions_positive_urlWithTrailingSlash() throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);

    mockHttpResponse(200, List.of(Map.of("id", "mod-foo-1.0.0")));

    var result = resolver.getAvailableVersions(okapiRegistryWithSlash(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.0.0");
    verify(log).debug("Module 'mod-foo' versions fetched from http://localhost/");
  }

  @Test
  void getAvailableVersions_positive_invalidModuleIdSkipped() throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);

    mockHttpResponse(200, List.of(
      Map.of("id", "mod-foo-1.0.0"),
      Map.of("id", "invalid-module-no-version")));

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.0.0");
    verify(log).debug("Module 'mod-foo' versions fetched from http://localhost");
  }

  @Test
  void getAvailableVersions_positive_retryOnSocketExceptionThenSuccess()
      throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);

    when(httpClient.send(any(HttpRequest.class), any()))
      .thenThrow(new SocketException("Connection reset"))
      .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(IOUtils.toInputStream("", "UTF-8"));
    when(jsonConverter.parse(any(InputStream.class), any()))
      .thenReturn(List.of(Map.of("id", "mod-foo-1.0.0")));

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.0.0");
    verify(log).warn("Network error, retrying (attempt 1): Connection reset");
    verify(log).debug("Module 'mod-foo' versions fetched from http://localhost");
  }

  @Test
  void getAvailableVersions_positive_retryOnSocketTimeoutExceptionThenSuccess()
      throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);

    when(httpClient.send(any(HttpRequest.class), any()))
      .thenThrow(new SocketTimeoutException("Read timed out"))
      .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(IOUtils.toInputStream("", "UTF-8"));
    when(jsonConverter.parse(any(InputStream.class), any()))
      .thenReturn(List.of(Map.of("id", "mod-foo-1.0.0")));

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.0.0");
    verify(log).warn("Network error, retrying (attempt 1): Read timed out");
    verify(log).debug("Module 'mod-foo' versions fetched from http://localhost");
  }

  @Test
  void getAvailableVersions_positive_retryOnHttpTimeoutExceptionThenSuccess()
      throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);

    when(httpClient.send(any(HttpRequest.class), any()))
      .thenThrow(new HttpTimeoutException("request timed out"))
      .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(IOUtils.toInputStream("", "UTF-8"));
    when(jsonConverter.parse(any(InputStream.class), any()))
      .thenReturn(List.of(Map.of("id", "mod-foo-1.0.0")));

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.0.0");
    verify(log).warn("Network error, retrying (attempt 1): request timed out");
    verify(log).debug("Module 'mod-foo' versions fetched from http://localhost");
  }

  @Test
  void getAvailableVersions_negative_httpTimeoutExceptionRetryLimitExhausted()
      throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var exception = new HttpTimeoutException("request timed out");
    var registry = okapiRegistry();

    when(httpClient.send(any(HttpRequest.class), any())).thenThrow(exception);

    assertThatThrownBy(() -> resolver.getAvailableVersions(registry, dependency, ModuleType.BE))
      .isInstanceOf(ApplicationGeneratorException.class)
      .hasMessageContaining("Network error while fetching versions for module 'mod-foo'")
      .hasCause(exception)
      .satisfies(e -> assertThat(((ApplicationGeneratorException) e).getCategory())
        .isEqualTo(ErrorCategory.INFRASTRUCTURE));

    verify(log).warn("Network error, retrying (attempt 1): request timed out");
    verify(log).warn("Network error, retrying (attempt 2): request timed out");
    verify(log).warn("Network error, retrying (attempt 3): request timed out");
    verify(log).warn("Network error, retrying (attempt 4): request timed out");
    verify(log).warn("Network error, retrying (attempt 5): request timed out");
    verify(log).warn("Failed to fetch versions for module 'mod-foo' from http://localhost", exception);
  }

  @Test
  void getAvailableVersions_negative_socketExceptionRetryLimitExhausted()
      throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var exception = new SocketException("Connection reset");
    var registry = okapiRegistry();

    when(httpClient.send(any(HttpRequest.class), any())).thenThrow(exception);

    assertThatThrownBy(() -> resolver.getAvailableVersions(registry, dependency, ModuleType.BE))
      .isInstanceOf(ApplicationGeneratorException.class)
      .hasMessageContaining("Network error while fetching versions for module 'mod-foo'")
      .hasCause(exception)
      .satisfies(e -> assertThat(((ApplicationGeneratorException) e).getCategory())
        .isEqualTo(ErrorCategory.INFRASTRUCTURE));

    verify(log).warn("Network error, retrying (attempt 1): Connection reset");
    verify(log).warn("Network error, retrying (attempt 2): Connection reset");
    verify(log).warn("Network error, retrying (attempt 3): Connection reset");
    verify(log).warn("Network error, retrying (attempt 4): Connection reset");
    verify(log).warn("Network error, retrying (attempt 5): Connection reset");
    verify(log).warn("Failed to fetch versions for module 'mod-foo' from http://localhost", exception);
  }

  @Test
  void getAvailableVersions_positive_retryOnStatusCodeThenSuccess()
      throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);

    when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);
    when(httpResponse.statusCode())
      .thenReturn(504)
      .thenReturn(200);
    when(httpResponse.body()).thenReturn(IOUtils.toInputStream("", "UTF-8"));
    when(jsonConverter.parse(any(InputStream.class), any()))
      .thenReturn(List.of(Map.of("id", "mod-foo-1.0.0")));

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isPresent();
    assertThat(result.get()).containsExactly("1.0.0");
    verify(log).debug("Retrying request due to status code 504 (attempt 1)");
    verify(log).debug("Module 'mod-foo' versions fetched from http://localhost");
  }

  @Test
  void getAvailableVersions_negative_statusCodeRetryLimitExhausted()
      throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);

    when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);
    when(httpResponse.statusCode())
      .thenReturn(504)
      .thenReturn(504)
      .thenReturn(504)
      .thenReturn(504)
      .thenReturn(504)
      .thenReturn(500);

    var result = resolver.getAvailableVersions(okapiRegistry(), dependency, ModuleType.BE);

    assertThat(result).isEmpty();
    verify(log).debug("Retrying request due to status code 504 (attempt 1)");
    verify(log).debug("Retrying request due to status code 504 (attempt 2)");
    verify(log).debug("Retrying request due to status code 504 (attempt 3)");
    verify(log).debug("Retrying request due to status code 504 (attempt 4)");
    verify(log).debug("Retrying request due to status code 504 (attempt 5)");
    verify(log).warn("Failed to fetch versions for module 'mod-foo' from http://localhost: HTTP 500");
  }

  @Test
  void getAvailableVersions_negative_genericExceptionReturnsEmpty()
      throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var exception = new RuntimeException("Unexpected error");
    var registry = okapiRegistry();

    when(httpClient.send(any(HttpRequest.class), any())).thenThrow(exception);

    var result = resolver.getAvailableVersions(registry, dependency, ModuleType.BE);

    assertThat(result).isEmpty();
    verify(log).warn("Failed to fetch versions for module 'mod-foo' from http://localhost", exception);
  }

  @Test
  void getAvailableVersions_negative_ioExceptionWithNullMessage()
      throws IOException, InterruptedException {
    var dependency = new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE);
    var exception = new IOException((String) null);
    var registry = okapiRegistry();

    when(httpClient.send(any(HttpRequest.class), any())).thenThrow(exception);

    assertThatThrownBy(() -> resolver.getAvailableVersions(registry, dependency, ModuleType.BE))
      .isInstanceOf(ApplicationGeneratorException.class)
      .hasMessageContaining("IOException")
      .hasCause(exception);
    verify(log).warn("Failed to fetch versions for module 'mod-foo' from http://localhost", exception);
  }

  private void mockHttpResponse(int statusCode, List<Map<String, Object>> payload)
      throws IOException, InterruptedException {
    when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(statusCode);

    if (payload != null) {
      when(httpResponse.body()).thenReturn(IOUtils.toInputStream("", "UTF-8"));
      when(jsonConverter.parse(any(InputStream.class), any())).thenReturn(payload);
    }
  }

  private static OkapiModuleRegistry okapiRegistry() {
    return new OkapiModuleRegistry().url(BASE_URL).withGeneratedFields();
  }

  private static OkapiModuleRegistry okapiRegistryWithSlash() {
    return new OkapiModuleRegistry().url(BASE_URL + "/").withGeneratedFields();
  }
}
