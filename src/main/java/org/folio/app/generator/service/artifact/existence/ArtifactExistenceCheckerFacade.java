package org.folio.app.generator.service.artifact.existence;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import org.folio.app.generator.conditions.ArtifactValidationCondition;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(ArtifactValidationCondition.class)
public class ArtifactExistenceCheckerFacade {

  private final Map<ModuleType, ArtifactExistenceChecker> checkersMap;

  @Autowired
  public ArtifactExistenceCheckerFacade(List<ArtifactExistenceChecker> checkers) {
    this.checkersMap = checkers.stream().collect(toMap(ArtifactExistenceChecker::getModuleType, identity()));
  }

  public boolean exists(ModuleDefinition module, ArtifactRegistry registry, ModuleType type) {
    var checker = checkersMap.get(type);
    if (checker == null) {
      throw new IllegalStateException(
        "No artifact existence checker found for module type: " + type
          + ". This indicates a configuration or programming error.");
    }

    return checker.exists(module, registry);
  }
}
