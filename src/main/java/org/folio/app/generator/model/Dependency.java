package org.folio.app.generator.model;

import lombok.Data;

@Data
public class Dependency {

  /**
   * Dependency name.
   */
  private String name;

  /**
   * Dependency version.
   */
  private String version;
}
