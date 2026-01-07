package org.folio.app.generator.model;

public record ErrorDetail(
  String errorType,
  String source,
  String artifact,
  String url,
  Integer httpStatusCode,
  String message
) {

  public static ErrorDetail moduleNotFound(String moduleName, String version, String constraint) {
    return new ErrorDetail("MODULE_NOT_FOUND", null, moduleName + "-" + version, null, null,
      String.format("No version matching constraint '%s' found", constraint));
  }

  public static ErrorDetail moduleNotFoundById(String moduleId) {
    return new ErrorDetail("MODULE_NOT_FOUND", null, moduleId, null, null,
      "Module descriptor not found in any registry");
  }

  public static ErrorDetail httpError(String url, int statusCode, String message) {
    return new ErrorDetail("HTTP_ERROR", null, null, url, statusCode, message);
  }

  public static ErrorDetail configurationError(String source, String message) {
    return new ErrorDetail("CONFIGURATION_ERROR", source, null, null, null, message);
  }

  public static ErrorDetail infrastructureError(String url, String message) {
    return new ErrorDetail("INFRASTRUCTURE", null, null, url, null, message);
  }
}
