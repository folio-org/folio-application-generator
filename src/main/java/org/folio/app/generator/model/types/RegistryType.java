package org.folio.app.generator.model.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RegistryType {

  AWS_S3("s3", "folio-app-generator.s3.enabled"),
  OKAPI("okapi", "folio-app-generator.okapi.enabled");

  private final String value;
  private final String propertyName;
}
