package org.folio.app.generator.model.types;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;

public enum ErrorCategory {
  NONE,
  INFRASTRUCTURE,
  MODULE_NOT_FOUND,
  ARTIFACT_NOT_FOUND,
  VALIDATION_FAILED,
  CONFIGURATION_ERROR;

  public static ErrorCategory fromException(Exception e) {
    if (e instanceof ApplicationGeneratorException age) {
      return age.getCategory();
    }
    if (e instanceof SocketException
        || e instanceof SocketTimeoutException
        || e instanceof UnknownHostException) {
      return INFRASTRUCTURE;
    }
    if (e instanceof IOException) {
      return CONFIGURATION_ERROR;
    }
    return INFRASTRUCTURE;
  }
}
