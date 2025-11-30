package org.folio.app.generator.service.artifact.existence;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ArtifactExistenceCheckerFacade {

  private final Log log;
  private final Map<ModuleType, ArtifactExistenceChecker> checkersMap;

  @Autowired
  public ArtifactExistenceCheckerFacade(Log log, List<ArtifactExistenceChecker> checkers) {
    this.log = log;
    this.checkersMap = checkers.stream().collect(toMap(ArtifactExistenceChecker::getModuleType, identity()));
  }

  public boolean exists(ModuleDefinition module, ArtifactRegistry registry, ModuleType type) {
    var checker = checkersMap.get(type);
    if (checker == null) {
      log.warn("Failed to find artifact existence checker for module type: " + type);
      return false;
    }

    return checker.exists(module, registry);
  }
}
