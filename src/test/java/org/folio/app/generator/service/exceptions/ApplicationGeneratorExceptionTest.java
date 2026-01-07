package org.folio.app.generator.service.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.folio.app.generator.model.ErrorDetail;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ApplicationGeneratorExceptionTest {

  @Test
  void constructor_withMessageAndCategory() {
    var exception = new ApplicationGeneratorException("Test message", ErrorCategory.MODULE_NOT_FOUND);

    assertThat(exception.getMessage()).isEqualTo("Test message");
    assertThat(exception.getCategory()).isEqualTo(ErrorCategory.MODULE_NOT_FOUND);
    assertThat(exception.getErrors()).isEmpty();
    assertThat(exception.getCause()).isNull();
  }

  @Test
  void constructor_withMessageCategoryAndCause() {
    var cause = new RuntimeException("Root cause");
    var exception = new ApplicationGeneratorException("Test message", ErrorCategory.INFRASTRUCTURE, cause);

    assertThat(exception.getMessage()).isEqualTo("Test message");
    assertThat(exception.getCategory()).isEqualTo(ErrorCategory.INFRASTRUCTURE);
    assertThat(exception.getErrors()).isEmpty();
    assertThat(exception.getCause()).isEqualTo(cause);
  }

  @Test
  void constructor_withSingleErrorDetail_notNull() {
    var errorDetail = ErrorDetail.moduleNotFound("mod-users", "1.0.0", "^1.0.0");
    var exception = new ApplicationGeneratorException("Test message", ErrorCategory.MODULE_NOT_FOUND, errorDetail);

    assertThat(exception.getMessage()).isEqualTo("Test message");
    assertThat(exception.getCategory()).isEqualTo(ErrorCategory.MODULE_NOT_FOUND);
    assertThat(exception.getErrors()).containsExactly(errorDetail);
  }

  @Test
  void constructor_withSingleErrorDetail_null() {
    ErrorDetail errorDetail = null;
    var exception = new ApplicationGeneratorException("Test message", ErrorCategory.MODULE_NOT_FOUND, errorDetail);

    assertThat(exception.getMessage()).isEqualTo("Test message");
    assertThat(exception.getCategory()).isEqualTo(ErrorCategory.MODULE_NOT_FOUND);
    assertThat(exception.getErrors()).isEmpty();
  }

  @Test
  void constructor_withErrorDetailList_notNull() {
    var error1 = ErrorDetail.moduleNotFoundById("mod-users-1.0.0");
    var error2 = ErrorDetail.moduleNotFoundById("mod-orders-2.0.0");
    var errors = List.of(error1, error2);
    var exception = new ApplicationGeneratorException("Test message", ErrorCategory.MODULE_NOT_FOUND, errors);

    assertThat(exception.getMessage()).isEqualTo("Test message");
    assertThat(exception.getCategory()).isEqualTo(ErrorCategory.MODULE_NOT_FOUND);
    assertThat(exception.getErrors()).containsExactly(error1, error2);
  }

  @Test
  void constructor_withErrorDetailList_null() {
    List<ErrorDetail> errors = null;
    var exception = new ApplicationGeneratorException("Test message", ErrorCategory.MODULE_NOT_FOUND, errors);

    assertThat(exception.getMessage()).isEqualTo("Test message");
    assertThat(exception.getCategory()).isEqualTo(ErrorCategory.MODULE_NOT_FOUND);
    assertThat(exception.getErrors()).isEmpty();
  }

  @Test
  void constructor_withErrorDetailList_empty() {
    List<ErrorDetail> errors = List.of();
    var exception = new ApplicationGeneratorException("Test message", ErrorCategory.CONFIGURATION_ERROR, errors);

    assertThat(exception.getMessage()).isEqualTo("Test message");
    assertThat(exception.getCategory()).isEqualTo(ErrorCategory.CONFIGURATION_ERROR);
    assertThat(exception.getErrors()).isEmpty();
  }
}
