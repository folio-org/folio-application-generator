package org.folio.app.generator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private PreReleaseFilter preRelease;

  /**
   * Optional flag (descriptor field).
   * When {@code true}, the dependency is optional and will not block entitlement if unavailable.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean optional;
}
