package org.folio.app.generator.conditions;

import static org.folio.app.generator.model.types.ArtifactRegistryType.FOLIO_NPM;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that matches when Folio NPM artifact validation is enabled.
 *
 * <p>This condition checks the system property: {@code folio-app-generator.folio-npm.enabled}
 */
public class FolioNpmCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    var folioNpmEnabled = context.getEnvironment().getProperty(FOLIO_NPM.getPropertyName());
    return Boolean.parseBoolean(folioNpmEnabled);
  }
}
