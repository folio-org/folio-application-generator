package org.folio.app.generator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Scope of an application: tenant-level or deployment-wide.
 */
public enum ApplicationScope {
  /**
   * Regular application, entitled per tenant.
   */
  REGULAR("regular"),

  /**
   * Deployment-wide application, only one version is active across the whole deployment.
   */
  PLATFORM("platform");

  private final String value;

  ApplicationScope(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static ApplicationScope fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (ApplicationScope scope : ApplicationScope.values()) {
      if (scope.value.equalsIgnoreCase(value)) {
        return scope;
      }
    }
    throw new IllegalArgumentException(
      "Invalid scope value: '" + value + "'. Allowed values: 'regular', 'platform'"
    );
  }
}
