package org.folio.app.generator.conditions;

import static org.folio.app.generator.model.types.ArtifactRegistryType.AWS_ECR;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that matches when AWS ECR artifact validation is enabled.
 *
 * <p>This condition checks the system property: {@code folio-app-generator.aws-ecr.enabled}
 */
public class EcrCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    var ecrEnabled = context.getEnvironment().getProperty(AWS_ECR.getPropertyName());
    return Boolean.parseBoolean(ecrEnabled);
  }
}
