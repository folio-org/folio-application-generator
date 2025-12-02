package org.folio.app.generator.model.registry.artifact;

import lombok.Data;

@Data
public class ConfigArtifactRegistry {

  private String type;

  private String baseUrl;

  private String namespace;
}
