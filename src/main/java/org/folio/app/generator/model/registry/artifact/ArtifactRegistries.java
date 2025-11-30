package org.folio.app.generator.model.registry.artifact;

import java.util.ArrayList;
import java.util.List;
import org.folio.app.generator.model.types.ModuleType;

public record ArtifactRegistries(
  List<ArtifactRegistry> beRegistries,
  List<ArtifactRegistry> uiRegistries,
  List<ArtifactRegistry> bePreReleaseRegistries,
  List<ArtifactRegistry> uiPreReleaseRegistries,
  List<ArtifactRegistry> unifiedRegistries
) {

  public List<ArtifactRegistry> getRegistries(ModuleType type, boolean isPreRelease) {
    var result = new ArrayList<ArtifactRegistry>();

    if (isPreRelease) {
      var preReleaseRegs = type == ModuleType.BE ? bePreReleaseRegistries : uiPreReleaseRegistries;
      if (preReleaseRegs != null && !preReleaseRegs.isEmpty()) {
        result.addAll(preReleaseRegs);
      }
    }

    var typeRegs = type == ModuleType.BE ? beRegistries : uiRegistries;
    if (typeRegs != null && !typeRegs.isEmpty()) {
      result.addAll(typeRegs);
    }

    if (unifiedRegistries != null && !unifiedRegistries.isEmpty()) {
      result.addAll(unifiedRegistries);
    }

    return result;
  }
}
