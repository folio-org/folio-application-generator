package org.folio.app.generator.model.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ErrorCategoryTest {

  @Test
  void fromException_applicationGeneratorException_returnsEmbeddedCategory() {
    var exception = new ApplicationGeneratorException("Test", ErrorCategory.MODULE_NOT_FOUND);

    var result = ErrorCategory.fromException(exception);

    assertThat(result).isEqualTo(ErrorCategory.MODULE_NOT_FOUND);
  }

  @Test
  void fromException_applicationGeneratorException_configurationError() {
    var exception = new ApplicationGeneratorException("Test", ErrorCategory.CONFIGURATION_ERROR);

    var result = ErrorCategory.fromException(exception);

    assertThat(result).isEqualTo(ErrorCategory.CONFIGURATION_ERROR);
  }

  @Test
  void fromException_socketException_returnsInfrastructure() {
    var exception = new SocketException("Connection reset");

    var result = ErrorCategory.fromException(exception);

    assertThat(result).isEqualTo(ErrorCategory.INFRASTRUCTURE);
  }

  @Test
  void fromException_socketTimeoutException_returnsInfrastructure() {
    var exception = new SocketTimeoutException("Read timed out");

    var result = ErrorCategory.fromException(exception);

    assertThat(result).isEqualTo(ErrorCategory.INFRASTRUCTURE);
  }

  @Test
  void fromException_unknownHostException_returnsInfrastructure() {
    var exception = new UnknownHostException("Unknown host");

    var result = ErrorCategory.fromException(exception);

    assertThat(result).isEqualTo(ErrorCategory.INFRASTRUCTURE);
  }

  @Test
  void fromException_ioException_returnsConfigurationError() {
    var exception = new IOException("File not found");

    var result = ErrorCategory.fromException(exception);

    assertThat(result).isEqualTo(ErrorCategory.CONFIGURATION_ERROR);
  }

  @Test
  void fromException_genericException_returnsInfrastructure() {
    var exception = new RuntimeException("Unknown error");

    var result = ErrorCategory.fromException(exception);

    assertThat(result).isEqualTo(ErrorCategory.INFRASTRUCTURE);
  }

  @Test
  void fromException_nullPointerException_returnsInfrastructure() {
    var exception = new NullPointerException("Null value");

    var result = ErrorCategory.fromException(exception);

    assertThat(result).isEqualTo(ErrorCategory.INFRASTRUCTURE);
  }

  @Test
  void fromException_illegalArgumentException_returnsInfrastructure() {
    var exception = new IllegalArgumentException("Invalid argument");

    var result = ErrorCategory.fromException(exception);

    assertThat(result).isEqualTo(ErrorCategory.INFRASTRUCTURE);
  }
}
