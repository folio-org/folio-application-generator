package org.folio.app.generator.conditions;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.context.annotation.Conditional;

/**
 * Condition that matches when HTTP client is needed. This is true when any of the following are enabled: - OKAPI module
 * registry - Simple module registry - Artifact validation (Docker Hub or Folio NPM)
 *
 * <p>This is a composite condition that checks:
 * <ul>
 *   <li>{@code folio-app-generator.okapi.enabled} (via {@link OkapiCondition})</li>
 *   <li>{@code folio-app-generator.simple.enabled} (via {@link SimpleCondition})</li>
 *   <li>{@code folio-app-generator.docker-hub.enabled} OR {@code folio-app-generator.folio-npm.enabled}
 *       (via {@link ArtifactValidationCondition})</li>
 * </ul>
 */
public class HttpCondition extends AnyNestedCondition {

  HttpCondition() {
    super(ConfigurationPhase.REGISTER_BEAN);
  }

  @Conditional(OkapiCondition.class)
  static class Okapi {}

  @Conditional(SimpleCondition.class)
  static class Simple {}

  @Conditional(ArtifactValidationCondition.class)
  static class ArtifactValidation {}
}
