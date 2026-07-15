package org.folio.app.generator.model.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ArtifactRegistryType {

  DOCKER_HUB("docker-hub", "folio-app-generator.docker-hub.enabled"),
  FOLIO_NPM("folio-npm", "folio-app-generator.folio-npm.enabled"),
  AWS_ECR("aws-ecr", "folio-app-generator.aws-ecr.enabled");

  private final String value;
  private final String propertyName;
}
