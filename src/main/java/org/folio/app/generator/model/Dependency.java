package org.folio.app.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dependency {

  /**
   * Dependency name.
   */
  private String name;

  /**
   * Dependency version.
   */
  private String version;

  /**
   * Pre-release filter (optional).
   * Determines whether to include pre-release versions when resolving dependencies.
   */
  private PreReleaseFilter preRelease;
}
