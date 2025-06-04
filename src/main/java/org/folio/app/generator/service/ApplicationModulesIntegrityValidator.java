package org.folio.app.generator.service;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.amazon.awssdk.http.HttpStatusCode.ACCEPTED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorCollection;
import org.springframework.stereotype.Component;

/**
 * Service for validating application module descriptors by sending them to a remote validator endpoint.
 */
@Component
@RequiredArgsConstructor
public class ApplicationModulesIntegrityValidator {
  private static final String VALIDATOR_PATH = "/applications/validate-descriptors";

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final Log log;

  /**
   * Validates a single application descriptor by sending it to the validator endpoint.
   *
   * @param descriptor the application descriptor to validate
   * @param baseUrl    the base URL of the validator service
   * @param token      the authentication token
   */
  public void validateApplication(ApplicationDescriptor descriptor, String baseUrl, String token) {
    if (descriptor == null || isBlank(descriptor.getId())) {
      throw new IllegalArgumentException("Application descriptor or its ID cannot be null or empty");
    }
    try {
      var applicationDescriptors = new ApplicationDescriptorCollection();
      applicationDescriptors.setApplicationDescriptors(singletonList(descriptor));

      log.info(format("Starting validation for application descriptor: %s", descriptor.getId()));

      var response = sendValidationRequest(applicationDescriptors, baseUrl, token);
      var responseStatus = response.statusCode();

      if (responseStatus != ACCEPTED) {
        log.error(format("Failed to validate application descriptor '%s'. Status code: %s", descriptor.getId(),
          responseStatus));
        if (isNotBlank(response.body())) {
          log.error(response.body());
        }
      } else {
        log.info(format("Application descriptor '%s' validated successfully.", descriptor.getId()));
      }
    } catch (Exception e) {
      log.error("An error occurred during validation for application descriptor: " + descriptor.getId(), e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Sends the validation request to the validator endpoint.
   *
   * @param applicationDescriptors the collection of application descriptors
   * @param baseUrl                the base URL of the validator service
   * @param token                  the authentication token
   * @return the HTTP response from the validator service
   */
  private HttpResponse<String> sendValidationRequest(ApplicationDescriptorCollection applicationDescriptors,
                                                     String baseUrl, String token)
    throws Exception {
    HttpRequest request = prepareHttpRequest(applicationDescriptors, baseUrl, token);
    log.info("Sending HTTP request to validate application descriptor");

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    log.info(format("Received response with status code: %d", response.statusCode()));

    return response;
  }

  /**
   * Prepares the HTTP request for validating application descriptors.
   *
   * @param descriptors the collection of application descriptors
   * @param baseUrl     the base URL of the validator service
   * @param token       the authentication token
   * @return the prepared HTTP request
   * @throws JsonProcessingException if serialization fails
   */
  private HttpRequest prepareHttpRequest(ApplicationDescriptorCollection descriptors, String baseUrl, String token)
    throws JsonProcessingException {
    baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    var url = baseUrl + VALIDATOR_PATH;
    var body = objectMapper.writeValueAsString(descriptors);

    if (body == null || body.isEmpty()) {
      throw new IllegalArgumentException("Serialized request body cannot be null or empty");
    }

    log.debug("Prepared HTTP request body: " + body);
    log.info("Prepared HTTP request to validate application descriptor: " + url);

    return HttpRequest.newBuilder()
      .POST(HttpRequest.BodyPublishers.ofString(body))
      .uri(URI.create(url))
      .timeout(Duration.ofMinutes(1))
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
      .version(HttpClient.Version.HTTP_1_1)
      .build();
  }
}
