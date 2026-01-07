package org.folio.app.generator.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ErrorDetailTest {

  @Test
  void moduleNotFound_createsCorrectErrorDetail() {
    var result = ErrorDetail.moduleNotFound("mod-users", "1.0.0", "^1.0.0");

    assertThat(result.errorType()).isEqualTo("MODULE_NOT_FOUND");
    assertThat(result.source()).isNull();
    assertThat(result.artifact()).isEqualTo("mod-users-1.0.0");
    assertThat(result.url()).isNull();
    assertThat(result.httpStatusCode()).isNull();
    assertThat(result.message()).isEqualTo("No version matching constraint '^1.0.0' found");
  }

  @Test
  void moduleNotFoundById_createsCorrectErrorDetail() {
    var result = ErrorDetail.moduleNotFoundById("mod-users-1.0.0");

    assertThat(result.errorType()).isEqualTo("MODULE_NOT_FOUND");
    assertThat(result.source()).isNull();
    assertThat(result.artifact()).isEqualTo("mod-users-1.0.0");
    assertThat(result.url()).isNull();
    assertThat(result.httpStatusCode()).isNull();
    assertThat(result.message()).isEqualTo("Module descriptor not found in any registry");
  }

  @Test
  void httpError_createsCorrectErrorDetail() {
    var result = ErrorDetail.httpError("https://example.com/api", 500, "Internal Server Error");

    assertThat(result.errorType()).isEqualTo("HTTP_ERROR");
    assertThat(result.source()).isNull();
    assertThat(result.artifact()).isNull();
    assertThat(result.url()).isEqualTo("https://example.com/api");
    assertThat(result.httpStatusCode()).isEqualTo(500);
    assertThat(result.message()).isEqualTo("Internal Server Error");
  }

  @Test
  void httpError_with404Status() {
    var result = ErrorDetail.httpError("https://registry.example.com/module", 404, "Not Found");

    assertThat(result.errorType()).isEqualTo("HTTP_ERROR");
    assertThat(result.httpStatusCode()).isEqualTo(404);
    assertThat(result.message()).isEqualTo("Not Found");
  }

  @Test
  void configurationError_createsCorrectErrorDetail() {
    var result = ErrorDetail.configurationError("/path/to/file.json", "File not found");

    assertThat(result.errorType()).isEqualTo("CONFIGURATION_ERROR");
    assertThat(result.source()).isEqualTo("/path/to/file.json");
    assertThat(result.artifact()).isNull();
    assertThat(result.url()).isNull();
    assertThat(result.httpStatusCode()).isNull();
    assertThat(result.message()).isEqualTo("File not found");
  }

  @Test
  void configurationError_withParseError() {
    var result = ErrorDetail.configurationError("pom.xml", "Invalid XML format");

    assertThat(result.errorType()).isEqualTo("CONFIGURATION_ERROR");
    assertThat(result.source()).isEqualTo("pom.xml");
    assertThat(result.message()).isEqualTo("Invalid XML format");
  }

  @Test
  void record_equality() {
    var error1 = ErrorDetail.moduleNotFoundById("mod-users-1.0.0");
    var error2 = ErrorDetail.moduleNotFoundById("mod-users-1.0.0");
    var error3 = ErrorDetail.moduleNotFoundById("mod-orders-2.0.0");

    assertThat(error1).isEqualTo(error2);
    assertThat(error1).isNotEqualTo(error3);
  }
}
