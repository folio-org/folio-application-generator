package org.folio.app.generator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
