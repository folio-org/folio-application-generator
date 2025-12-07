package org.folio.app.generator.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateConfig {

  @Builder.Default
  private final boolean allowDowngrade = false;

  @Builder.Default
  private final boolean allowAddModules = false;

  @Builder.Default
  private final boolean removeUnlistedModules = false;

  @Builder.Default
  private final boolean useProjectVersion = false;

  public static UpdateConfig defaults() {
    return UpdateConfig.builder().build();
  }
}
