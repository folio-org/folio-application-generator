package org.folio.app.generator.service.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.SimpleModuleRegistry;
import org.folio.app.generator.model.types.RegistryType;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.JsonConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SimpleModuleDescriptorLoaderTest {

  private static final String URL = "http://localhost/mod-foo-1.0.0";
  
  @InjectMocks private SimpleModuleDescriptorLoader loader;
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
    assertThat(loader.getType()).isEqualTo(RegistryType.SIMPLE);
  }

  @ParameterizedTest(name = "[{index}] retry = {0}, extraPath = {1}")
  @MethodSource("statusPathRetrySuccessSource")
  void findModuleDescriptor_positive_emptyResult(int retry, String extraPath)
      throws IOException, InterruptedException {

    mockStatusResponse(200, retry);
    mockPayloadResponse(new HashMap<String, Object>());

    var result = loader.findModuleDescriptor(simpleRegistry(extraPath), fooModule("1.0.0"));

    assertEmptyAndWarnLog(result,
      "Module descriptor 'mod-foo-1.0.0' is not found in http://localhost/mod-foo-1.0.0");
  }

  @ParameterizedTest(name = "[{index}] retry = {0}, extraPath = {1}")
  @MethodSource("statusPathRetrySuccessSource")
  void findModuleDescriptor_positive_singleModuleDescriptor(int retry, String extraPath)
      throws IOException, InterruptedException {

    var expectedModuleDescriptor = fooModuleDescriptor("1.0.0");

    mockStatusResponse(200, retry);
    mockPayloadResponse(expectedModuleDescriptor);

    var result = loader.findModuleDescriptor(simpleRegistry(extraPath), fooModule("1.0.0"));

    assertTrue(result.isPresent());
    assertThat(result.get().getModuleDescriptor()).containsAllEntriesOf(expectedModuleDescriptor);
    assertThat(result.get().getSourceUrl().toString()).isEqualTo(URL);

    verify(log).info("Module descriptor 'mod-foo-1.0.0' loaded from http://localhost/mod-foo-1.0.0");
  }

  @ParameterizedTest(name = "[{index}] statusCode = {0}, retry = {1}, extraPath = {2}")
  @MethodSource("statusPathRetryFailSource")
  void findModuleDescriptor_negative_loadFail(int statusCode, int retry, String extraPath)
      throws IOException, InterruptedException {

    mockStatusResponse(statusCode, retry);

    var result = loader.findModuleDescriptor(simpleRegistry(extraPath), fooModule("1.0.0"));

    assertEmptyAndWarnLog(result,
      "Failed to load module descriptor 'mod-foo-1.0.0' from http://localhost/mod-foo-1.0.0: " + statusCode);
  }

  @Test
  void findModuleDescriptor_negative_throwsException() throws IOException, InterruptedException {
    ReflectionTestUtils.setField(loader, "httpClient", httpClient);

    var exception = new IOException("This is a mocked exception.");
    when(httpClient.send(any(HttpRequest.class), any())).thenThrow(exception);

    var result = loader.findModuleDescriptor(simpleRegistry(""), fooModule("1.0.0"));

    assertThat(result).isEmpty();
    verify(log)
      .warn("Failed to load module descriptor 'mod-foo-1.0.0' from http://localhost/mod-foo-1.0.0", exception);
  }

  @Test
  void findModuleDescriptor_positive_retryOnSocketExceptionThenSuccess()
      throws IOException, InterruptedException {
    ReflectionTestUtils.setField(loader, "httpClient", httpClient);
    var expectedModuleDescriptor = fooModuleDescriptor("1.0.0");

    when(httpClient.send(any(HttpRequest.class), any()))
      .thenThrow(new SocketException("Connection reset"))
      .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    mockPayloadResponse(expectedModuleDescriptor);

    var result = loader.findModuleDescriptor(simpleRegistry(""), fooModule("1.0.0"));

    assertTrue(result.isPresent());
    assertThat(result.get().getModuleDescriptor()).containsAllEntriesOf(expectedModuleDescriptor);
    verify(log).warn("Network error, retrying (attempt 1): Connection reset");
    verify(log).info("Module descriptor 'mod-foo-1.0.0' loaded from http://localhost/mod-foo-1.0.0");
  }

  @Test
  void findModuleDescriptor_positive_retryOnSocketTimeoutExceptionThenSuccess()
      throws IOException, InterruptedException {
    ReflectionTestUtils.setField(loader, "httpClient", httpClient);
    var expectedModuleDescriptor = fooModuleDescriptor("1.0.0");

    when(httpClient.send(any(HttpRequest.class), any()))
      .thenThrow(new SocketTimeoutException("Read timed out"))
      .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    mockPayloadResponse(expectedModuleDescriptor);

    var result = loader.findModuleDescriptor(simpleRegistry(""), fooModule("1.0.0"));

    assertTrue(result.isPresent());
    assertThat(result.get().getModuleDescriptor()).containsAllEntriesOf(expectedModuleDescriptor);
    verify(log).warn("Network error, retrying (attempt 1): Read timed out");
    verify(log).info("Module descriptor 'mod-foo-1.0.0' loaded from http://localhost/mod-foo-1.0.0");
  }

  @Test
  void findModuleDescriptor_negative_socketExceptionRetryLimitExhausted()
      throws IOException, InterruptedException {
    ReflectionTestUtils.setField(loader, "httpClient", httpClient);

    var exception = new SocketException("Connection reset");
    when(httpClient.send(any(HttpRequest.class), any())).thenThrow(exception);

    var result = loader.findModuleDescriptor(simpleRegistry(""), fooModule("1.0.0"));

    assertThat(result).isEmpty();
    verify(log).warn("Network error, retrying (attempt 1): Connection reset");
    verify(log).warn("Network error, retrying (attempt 2): Connection reset");
    verify(log).warn("Network error, retrying (attempt 3): Connection reset");
    verify(log).warn("Network error, retrying (attempt 4): Connection reset");
    verify(log).warn("Network error, retrying (attempt 5): Connection reset");
    verify(log)
      .warn("Failed to load module descriptor 'mod-foo-1.0.0' from http://localhost/mod-foo-1.0.0", exception);
  }

  @Test
  void findModuleDescriptor_positive_retryOnStatusCodeThenSuccess()
      throws IOException, InterruptedException {
    ReflectionTestUtils.setField(loader, "httpClient", httpClient);
    var expectedModuleDescriptor = fooModuleDescriptor("1.0.0");

    when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);
    when(httpResponse.statusCode())
      .thenReturn(504).thenReturn(504)
      .thenReturn(200);
    mockPayloadResponse(expectedModuleDescriptor);

    var result = loader.findModuleDescriptor(simpleRegistry(""), fooModule("1.0.0"));

    assertTrue(result.isPresent());
    assertThat(result.get().getModuleDescriptor()).containsAllEntriesOf(expectedModuleDescriptor);
    verify(log).debug("Retrying request due to status code 504 (attempt 1)");
    verify(log).info("Module descriptor 'mod-foo-1.0.0' loaded from http://localhost/mod-foo-1.0.0");
  }

  @Test
  void findModuleDescriptor_negative_statusCodeRetryLimitExhausted()
      throws IOException, InterruptedException {
    ReflectionTestUtils.setField(loader, "httpClient", httpClient);

    when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);
    when(httpResponse.statusCode())
      .thenReturn(504).thenReturn(504)
      .thenReturn(504).thenReturn(504)
      .thenReturn(504).thenReturn(504)
      .thenReturn(504).thenReturn(504)
      .thenReturn(504).thenReturn(504)
      .thenReturn(404);

    var result = loader.findModuleDescriptor(simpleRegistry(""), fooModule("1.0.0"));

    assertThat(result).isEmpty();
    verify(log).debug("Retrying request due to status code 504 (attempt 1)");
    verify(log).debug("Retrying request due to status code 504 (attempt 2)");
    verify(log).debug("Retrying request due to status code 504 (attempt 3)");
    verify(log).debug("Retrying request due to status code 504 (attempt 4)");
    verify(log).debug("Retrying request due to status code 504 (attempt 5)");
    verify(log).warn("Failed to load module descriptor 'mod-foo-1.0.0' from http://localhost/mod-foo-1.0.0: 404");
  }

  public static Stream<Arguments> statusPathRetryFailSource() {
    return Stream.of(
      arguments(204, 0, ""),
      arguments(404, 0, ""),
      arguments(204, 1, ""),
      arguments(404, 1, ""),
      arguments(204, 6, ""),
      arguments(404, 6, ""),
      arguments(204, 0, "/"),
      arguments(404, 0, "/"),
      arguments(204, 1, "/"),
      arguments(404, 1, "/"),
      arguments(204, 6, "/"),
      arguments(404, 6, "/")
    );
  }

  public static Stream<Arguments> statusPathRetrySuccessSource() {
    return Stream.of(
      arguments(0, ""),
      arguments(1, ""),
      arguments(6, ""),
      arguments(0, "/"),
      arguments(1, "/"),
      arguments(6, "/")
    );
  }

  private void assertEmptyAndWarnLog(Optional<LoaderResultContainer> result, String message) {
    assertThat(result).isEmpty();
    verify(log).warn(message);
  }

  private void mockStatusResponse(int statusCode, int retry) throws IOException, InterruptedException {
    ReflectionTestUtils.setField(loader, "httpClient", httpClient);

    when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

    for (int i = 0; i < retry; i++) {
      when(httpResponse.statusCode()).thenReturn(504);
    }

    when(httpResponse.statusCode()).thenReturn(statusCode);
    when(httpResponse.statusCode()).thenReturn(statusCode);
  }

  private void mockPayloadResponse(Object payload) {
    when(httpResponse.body()).thenReturn(IOUtils.toInputStream("", "UTF-8"));
    when(jsonConverter.parse(any(InputStream.class), any())).thenReturn(payload);
  }

  private static Map<String, Object> fooModuleDescriptor(String version) {
    return Map.of(
      "id", "mod-foo" + "-" + version,
      "name", "Test name for module: " + "mod-foo",
      "description", "A description for module: " + "mod-foo"
    );
  }

  private static ModuleDefinition fooModule(String version) {
    return new ModuleDefinition().id("mod-foo-" + version).name("mod-foo").version(version);
  }

  private static SimpleModuleRegistry simpleRegistry(String extra) {
    return new SimpleModuleRegistry()
      .url("http://localhost" + extra)
      .withGeneratedFields();
  }
}
