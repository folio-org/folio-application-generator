package org.folio.app.generator.service.artifact.existence;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import org.folio.app.generator.conditions.ArtifactValidationCondition;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistry;
import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(ArtifactValidationCondition.class)
public class ArtifactExistenceCheckerFacade {

  private final Map<ArtifactRegistryType, ArtifactExistenceChecker> checkersMap;

  @Autowired
  public ArtifactExistenceCheckerFacade(List<ArtifactExistenceChecker> checkers) {
    this.checkersMap = checkers.stream().collect(toMap(ArtifactExistenceChecker::getRegistryType, identity()));
  }

  public boolean exists(ModuleDefinition module, ArtifactRegistry registry) {
    var checker = checkersMap.get(registry.getType());
    if (checker == null) {
      throw new IllegalStateException(
        "No artifact existence checker found for registry type: " + registry.getType()
          + ". This indicates a configuration or programming error.");
    }

    return checker.exists(module, registry);
  }
}
