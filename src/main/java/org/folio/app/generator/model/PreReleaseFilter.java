package org.folio.app.generator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Pre-release filter options for module dependencies.
 */
public enum PreReleaseFilter {
  /**
   * Include only pre-release versions.
   */
  ONLY("only"),

  /**
   * Include pre-release versions along with stable versions.
   */
  TRUE("true"),

  /**
   * Exclude pre-release versions (only stable versions).
   */
  FALSE("false");

  private final String value;

  PreReleaseFilter(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  public boolean isPreRelease() {
    return this == ONLY || this == TRUE;
  }

  @JsonCreator
  public static PreReleaseFilter fromValue(String value) {
    if (value == null) {
      return null;
    }
    for (PreReleaseFilter filter : PreReleaseFilter.values()) {
      if (filter.value.equalsIgnoreCase(value)) {
        return filter;
      }
    }
    throw new IllegalArgumentException(
      "Invalid preRelease value: '" + value + "'. Allowed values: 'only', 'true', 'false'"
    );
  }
}
