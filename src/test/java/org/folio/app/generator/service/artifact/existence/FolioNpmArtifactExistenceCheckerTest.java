package org.folio.app.generator.service.artifact.existence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.FolioNpmArtifactRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.support.UnitTest;
import org.folio.app.generator.utils.JsonConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class FolioNpmArtifactExistenceCheckerTest {

  @Mock private HttpClient httpClient;
  @Mock private Log log;
  @Mock private JsonConverter jsonConverter;
  @Mock private HttpResponse<InputStream> httpResponse;

  private FolioNpmArtifactExistenceChecker checker;

  @BeforeEach
  void setUp() {
    checker = new FolioNpmArtifactExistenceChecker(httpClient, log, jsonConverter);
  }

  @Test
  void getModuleType_positive() {
    assertThat(checker.getModuleType()).isEqualTo(ModuleType.UI);
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_positive_packageFound() throws Exception {
    var module = new ModuleDefinition().name("folio_users").version("1.0.0");
    var registry = new FolioNpmArtifactRegistry().namespace("npm-folio");
    var responseBody = new ByteArrayInputStream("{}".getBytes());

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseBody);
    when(jsonConverter.parse(any(InputStream.class), any(TypeReference.class)))
      .thenReturn(Map.of("versions", Map.of("1.0.0", Map.of())));

    var result = checker.exists(module, registry);

    assertThat(result).isTrue();
    verify(log).debug("NPM package found: @folio/users@1.0.0");
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_negative_packageNotFound() throws Exception {
    var module = new ModuleDefinition().name("folio_users").version("1.0.0");
    var registry = new FolioNpmArtifactRegistry().namespace("npm-folio");

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(404);

    var result = checker.exists(module, registry);

    assertThat(result).isFalse();
    verify(log).debug("NPM package not found: @folio/users (status: 404)");
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_negative_versionNotFound() throws Exception {
    var module = new ModuleDefinition().name("folio_users").version("2.0.0");
    var registry = new FolioNpmArtifactRegistry().namespace("npm-folio");
    var responseBody = new ByteArrayInputStream("{}".getBytes());

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseBody);
    when(jsonConverter.parse(any(InputStream.class), any(TypeReference.class)))
      .thenReturn(Map.of("versions", Map.of("1.0.0", Map.of())));

    var result = checker.exists(module, registry);

    assertThat(result).isFalse();
    verify(log).debug("NPM package version not found: @folio/users@2.0.0");
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_negative_versionsNull() throws Exception {
    var module = new ModuleDefinition().name("folio_users").version("1.0.0");
    var registry = new FolioNpmArtifactRegistry().namespace("npm-folio");
    var responseBody = new ByteArrayInputStream("{}".getBytes());

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseBody);
    when(jsonConverter.parse(any(InputStream.class), any(TypeReference.class)))
      .thenReturn(Map.of());

    var result = checker.exists(module, registry);

    assertThat(result).isFalse();
    verify(log).debug("NPM package version not found: @folio/users@1.0.0");
  }

  @ParameterizedTest
  @CsvSource({
    "folio_users, @folio/users",
    "folio_inventory, @folio/inventory",
    "folio_circulation, @folio/circulation",
    "folio_organizations, @folio/organizations"
  })
  void transformModuleNameToPackage_positive_folioPrefix(String moduleName, String expected) {
    var result = FolioNpmArtifactExistenceChecker.transformModuleNameToPackage(moduleName);
    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "ui_users, @folio/ui-users",
    "ui_inventory, @folio/ui-inventory",
    "platform_core, @folio/platform-core"
  })
  void transformModuleNameToPackage_positive_otherPrefix(String moduleName, String expected) {
    var result = FolioNpmArtifactExistenceChecker.transformModuleNameToPackage(moduleName);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void transformModuleNameToPackage_positive_underscoreToHyphen() {
    var result = FolioNpmArtifactExistenceChecker.transformModuleNameToPackage("folio_service_interaction");

    assertThat(result).isEqualTo("@folio/service-interaction");
  }

  @Test
  void transformModuleNameToPackage_positive_multipleUnderscores() {
    var result = FolioNpmArtifactExistenceChecker.transformModuleNameToPackage("folio_some_long_module_name");

    assertThat(result).isEqualTo("@folio/some-long-module-name");
  }

  @Test
  void transformModuleNameToPackage_positive_noUnderscores() {
    var result = FolioNpmArtifactExistenceChecker.transformModuleNameToPackage("foliousers");

    assertThat(result).isEqualTo("@folio/foliousers");
  }

  @Test
  void transformModuleNameToPackage_positive_alreadyHasHyphens() {
    var result = FolioNpmArtifactExistenceChecker.transformModuleNameToPackage("folio_user-management");

    assertThat(result).isEqualTo("@folio/user-management");
  }

  @Test
  @SuppressWarnings("unchecked")
  void exists_negative_httpClientThrowsException() throws Exception {
    var module = new ModuleDefinition().name("folio_users").version("1.0.0");
    var registry = new FolioNpmArtifactRegistry().namespace("npm-folio");

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
      .thenThrow(new IOException("Connection refused"));

    assertThatThrownBy(() -> checker.exists(module, registry))
      .isInstanceOf(IOException.class)
      .hasMessage("Connection refused");
  }
}
