package org.folio.app.generator.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationModulesIntegrityValidatorTest {
  private static final String BASE_URL = "http://validator-service";
  private static final String TOKEN = "test-token";

  @Mock
  private ObjectMapper objectMapper;
  @Mock
  private HttpClient httpClient;
  @Mock
  private Log log;

  @InjectMocks
  private ApplicationModulesIntegrityValidator validator;

  @Test
  void validateApplication_shouldLogStartAndEnd() throws IOException, InterruptedException {
    var descriptor = new ApplicationDescriptor();
    descriptor.setId("test-app");

    var descriptorCollection = new ApplicationDescriptorCollection();
    descriptorCollection.setApplicationDescriptors(List.of(descriptor));
    when(objectMapper.writeValueAsString(descriptorCollection)).thenReturn(
      "{\"applicationDescriptors\":[{\"id\":\"test-app\"}]}");

    doNothing().when(log).info(anyString());

    var mockResponse = mock(HttpResponse.class);
    when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(mockResponse);
    when(mockResponse.body()).thenReturn("Mock response body");

    validator.validateApplication(descriptor, BASE_URL, TOKEN);

    verify(log).info(contains("Starting validation for application descriptor: test-app"));
  }

  @Test
  void validateApplication_shouldSendHttpRequest() throws Exception {
    var descriptor = new ApplicationDescriptor();
    descriptor.setId("test-app");

    var descriptorCollection = new ApplicationDescriptorCollection();
    descriptorCollection.setApplicationDescriptors(List.of(descriptor));
    when(objectMapper.writeValueAsString(descriptorCollection)).thenReturn(
      "{\"applicationDescriptors\":[{\"id\":\"test-app\"}]}");

    var mockResponse = mock(HttpResponse.class);
    when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
      .thenReturn(mockResponse);
    when(mockResponse.body()).thenReturn("Mock response body");

    validator.validateApplication(descriptor, BASE_URL, TOKEN);

    var expectedRequest = HttpRequest.newBuilder()
      .uri(URI.create(BASE_URL + "/applications/validate-descriptors"))
      .header("Authorization", "Bearer " + TOKEN)
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString("{\"applicationDescriptors\":[{\"id\":\"test-app\"}]}"))
      .build();

    verify(httpClient).send(eq(expectedRequest), eq(HttpResponse.BodyHandlers.ofString()));
  }

  @Test
  void validateApplication_shouldHandleJsonProcessingException() throws Exception {
    var descriptor = new ApplicationDescriptor();
    descriptor.setId("test-app");

    when(objectMapper.writeValueAsString(descriptor)).thenThrow(new JsonProcessingException("Error") {
    });

    assertThrows(RuntimeException.class, () -> validator.validateApplication(descriptor, BASE_URL, TOKEN));
  }

  @Test
  void validateApplication_shouldHandleHttpException() throws Exception {
    var descriptor = new ApplicationDescriptor();
    descriptor.setId("test-app");

    var requestBody = "{\"id\":\"test-app\"}";
    when(objectMapper.writeValueAsString(descriptor)).thenReturn(requestBody);

    when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
      .thenThrow(new IOException("HTTP error"));

    assertThrows(RuntimeException.class, () -> validator.validateApplication(descriptor, BASE_URL, TOKEN));
  }

  @Test
  void validateApplication_shouldThrowExceptionForNullDescriptor() {
    assertThrows(IllegalArgumentException.class, () -> validator.validateApplication(null, BASE_URL, TOKEN));
  }

  @Test
  void validateApplication_shouldThrowExceptionForEmptyDescriptorId() {
    var descriptor = new ApplicationDescriptor();
    descriptor.setId("");

    assertThrows(IllegalArgumentException.class, () -> validator.validateApplication(descriptor, BASE_URL, TOKEN));
  }
}
