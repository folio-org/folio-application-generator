package org.folio.app.generator.model.registry.artifact;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.net.MalformedURLException;
import java.net.URL;
import lombok.EqualsAndHashCode;
import org.folio.app.generator.model.types.ArtifactRegistryType;

@EqualsAndHashCode(callSuper = true)
public class EcrArtifactRegistry extends AbstractArtifactRegistry<EcrArtifactRegistry> {

  private static final ArtifactRegistryType TYPE = ArtifactRegistryType.AWS_ECR;

  public EcrArtifactRegistry() {
    super(null);
  }

  @Override
  public ArtifactRegistryType getType() {
    return TYPE;
  }

  @Override
  public boolean isValid() {
    if (isBlank(baseUrl)) {
      return false;
    }
    try {
      new URL(baseUrl);
      return true;
    } catch (MalformedURLException e) {
      return false;
    }
  }
}
