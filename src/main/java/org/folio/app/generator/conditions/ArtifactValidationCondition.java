package org.folio.app.generator.conditions;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.context.annotation.Conditional;

/**
 * Condition that matches when any artifact validation is enabled. This condition is true if either Docker Hub or Folio
 * NPM artifact validation is enabled.
 *
 * <p>This is a composite condition that checks:
 * <ul>
 *   <li>{@code folio-app-generator.docker-hub.enabled} (via {@link DockerHubCondition})</li>
 *   <li>{@code folio-app-generator.folio-npm.enabled} (via {@link FolioNpmCondition})</li>
 * </ul>
 */
public class ArtifactValidationCondition extends AnyNestedCondition {

  ArtifactValidationCondition() {
    super(ConfigurationPhase.REGISTER_BEAN);
  }

  @Conditional(DockerHubCondition.class)
  static class DockerHub {}

  @Conditional(FolioNpmCondition.class)
  static class FolioNpm {}
}
