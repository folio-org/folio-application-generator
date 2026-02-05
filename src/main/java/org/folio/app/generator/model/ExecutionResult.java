package org.folio.app.generator.model;

import java.util.List;
import org.folio.app.generator.model.types.ErrorCategory;

public record ExecutionResult(
  String status,
  Boolean success,
  String goal,
  String appName,
  String appVersion,
  String errorCategory,
  String errorMessage,
  List<ErrorDetail> errors,
  Boolean changesDetected
) {

  public static ExecutionResult started(String goal, String appName) {
    return new ExecutionResult("STARTED", null, goal, appName, null, null, null, List.of(), null);
  }

  public static ExecutionResult success(String goal, String appName, String appVersion,
                                        boolean changesDetected) {
    return new ExecutionResult("COMPLETED", true, goal, appName, appVersion,
      ErrorCategory.NONE.name(), null, List.of(), changesDetected);
  }

  public static ExecutionResult failure(String goal, String appName, ErrorCategory category,
                                        String message, List<ErrorDetail> errors) {
    return new ExecutionResult("COMPLETED", false, goal, appName, null,
      category.name(), message, errors != null ? errors : List.of(), null);
  }
}
