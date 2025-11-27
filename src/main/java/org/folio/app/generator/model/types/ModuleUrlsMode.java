package org.folio.app.generator.model.types;

import java.util.Locale;

public enum ModuleUrlsMode {
  FALSE,
  TRUE,
  BOTH;

  public static ModuleUrlsMode fromString(String value) {
    if (value == null) {
      return FALSE;
    }

    return switch (value.toLowerCase(Locale.ROOT)) {
      case "true" -> TRUE;
      case "both" -> BOTH;
      default -> FALSE;
    };
  }

  public boolean needDescriptorUrl() {
    return this == TRUE || this == BOTH;
  }

  public boolean needFullDescriptor() {
    return this == FALSE || this == BOTH;
  }

  public boolean needBoth() {
    return this == BOTH;
  }
}
