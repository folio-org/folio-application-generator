package org.folio.app.generator.conditions;

import static org.folio.app.generator.model.types.ArtifactRegistryType.DOCKER_HUB;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that matches when Docker Hub artifact validation is enabled.
 *
 * <p>This condition checks the system property: {@code folio-app-generator.docker-hub.enabled}
 */
public class DockerHubCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    var dockerHubEnabled = context.getEnvironment().getProperty(DOCKER_HUB.getPropertyName());
    return Boolean.parseBoolean(dockerHubEnabled);
  }
}
