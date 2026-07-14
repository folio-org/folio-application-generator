package org.folio.app.generator.model.registry;

import lombok.Data;
import lombok.ToString;

@Data
public class ConfigHeader {

  /**
   * HTTP header name.
   */
  private String name;

  /**
   * HTTP header value (may be a secret supplied via Maven property/environment interpolation).
   */
  @ToString.Exclude
  private String value;
}
