package org.folio.app.generator.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ExecutionResultTest {

  @Test
  void started_createsCorrectResult() {
    var result = ExecutionResult.started("generateFromJson", "app-platform");

    assertThat(result.status()).isEqualTo("STARTED");
    assertThat(result.success()).isNull();
    assertThat(result.goal()).isEqualTo("generateFromJson");
    assertThat(result.appName()).isEqualTo("app-platform");
    assertThat(result.appVersion()).isNull();
    assertThat(result.errorCategory()).isNull();
    assertThat(result.errorMessage()).isNull();
    assertThat(result.errors()).isEmpty();
    assertThat(result.changesDetected()).isNull();
  }

  @Test
  void success_withChangesDetected_createsCorrectResult() {
    var result = ExecutionResult.success("generateFromJson", "app-platform", "1.0.0", true);

    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(result.success()).isTrue();
    assertThat(result.goal()).isEqualTo("generateFromJson");
    assertThat(result.appName()).isEqualTo("app-platform");
    assertThat(result.appVersion()).isEqualTo("1.0.0");
    assertThat(result.errorCategory()).isEqualTo("NONE");
    assertThat(result.errorMessage()).isNull();
    assertThat(result.errors()).isEmpty();
    assertThat(result.changesDetected()).isTrue();
  }

  @Test
  void success_withNoChangesDetected_createsCorrectResult() {
    var result = ExecutionResult.success("updateFromJson", "app-platform", "1.0.0", false);

    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(result.success()).isTrue();
    assertThat(result.goal()).isEqualTo("updateFromJson");
    assertThat(result.appName()).isEqualTo("app-platform");
    assertThat(result.appVersion()).isEqualTo("1.0.0");
    assertThat(result.errorCategory()).isEqualTo("NONE");
    assertThat(result.errorMessage()).isNull();
    assertThat(result.errors()).isEmpty();
    assertThat(result.changesDetected()).isFalse();
  }

  @Test
  void failure_withErrors() {
    var error1 = ErrorDetail.moduleNotFoundById("mod-users-1.0.0");
    var error2 = ErrorDetail.moduleNotFoundById("mod-orders-2.0.0");
    var errors = List.of(error1, error2);

    var result = ExecutionResult.failure("generateFromJson", "app-platform",
      ErrorCategory.MODULE_NOT_FOUND, "Failed to load modules", errors);

    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(result.success()).isFalse();
    assertThat(result.goal()).isEqualTo("generateFromJson");
    assertThat(result.appName()).isEqualTo("app-platform");
    assertThat(result.appVersion()).isNull();
    assertThat(result.errorCategory()).isEqualTo("MODULE_NOT_FOUND");
    assertThat(result.errorMessage()).isEqualTo("Failed to load modules");
    assertThat(result.errors()).containsExactly(error1, error2);
    assertThat(result.changesDetected()).isNull();
  }

  @Test
  void failure_withNullErrors() {
    var result = ExecutionResult.failure("generateFromJson", "app-platform",
      ErrorCategory.INFRASTRUCTURE, "Network error", null);

    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(result.success()).isFalse();
    assertThat(result.errorCategory()).isEqualTo("INFRASTRUCTURE");
    assertThat(result.errorMessage()).isEqualTo("Network error");
    assertThat(result.errors()).isEmpty();
  }

  @Test
  void failure_withEmptyErrors() {
    var result = ExecutionResult.failure("updateFromJson", "app-finc",
      ErrorCategory.CONFIGURATION_ERROR, "Invalid template", List.of());

    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(result.success()).isFalse();
    assertThat(result.errorCategory()).isEqualTo("CONFIGURATION_ERROR");
    assertThat(result.errors()).isEmpty();
  }

  @Test
  void failure_withDifferentGoals() {
    var result1 = ExecutionResult.failure("generateFromJson", "app", ErrorCategory.MODULE_NOT_FOUND, "err", null);
    var result2 = ExecutionResult.failure("updateFromTemplate", "app", ErrorCategory.MODULE_NOT_FOUND, "err", null);
    var result3 = ExecutionResult.failure("validateIntegrity", "app", ErrorCategory.VALIDATION_FAILED, "err", null);

    assertThat(result1.goal()).isEqualTo("generateFromJson");
    assertThat(result2.goal()).isEqualTo("updateFromTemplate");
    assertThat(result3.goal()).isEqualTo("validateIntegrity");
    assertThat(result3.errorCategory()).isEqualTo("VALIDATION_FAILED");
  }
}
