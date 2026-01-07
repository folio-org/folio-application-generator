package org.folio.app.generator.service.exceptions;

import java.util.List;
import lombok.Getter;
import org.folio.app.generator.model.ErrorDetail;
import org.folio.app.generator.model.types.ErrorCategory;

@Getter
public class ApplicationGeneratorException extends RuntimeException {

  private final ErrorCategory category;
  private final transient List<ErrorDetail> errors;

  public ApplicationGeneratorException(String message, ErrorCategory category) {
    super(message);
    this.category = category;
    this.errors = List.of();
  }

  public ApplicationGeneratorException(String message, ErrorCategory category, Throwable cause) {
    super(message, cause);
    this.category = category;
    this.errors = List.of();
  }

  public ApplicationGeneratorException(String message, ErrorCategory category, ErrorDetail error, Throwable cause) {
    super(message, cause);
    this.category = category;
    this.errors = error != null ? List.of(error) : List.of();
  }

  public ApplicationGeneratorException(String message, ErrorCategory category, ErrorDetail error) {
    super(message);
    this.category = category;
    this.errors = error != null ? List.of(error) : List.of();
  }

  public ApplicationGeneratorException(String message, ErrorCategory category, List<ErrorDetail> errors) {
    super(message);
    this.category = category;
    this.errors = errors != null ? errors : List.of();
  }
}
