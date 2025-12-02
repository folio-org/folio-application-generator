package org.folio.app.generator.model.registry.artifact;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.net.MalformedURLException;
import java.net.URL;
import lombok.Data;

@Data
public abstract class AbstractArtifactRegistry<T extends AbstractArtifactRegistry<T>>
    implements ArtifactRegistry {

  protected String baseUrl;
  protected String namespace;

  protected AbstractArtifactRegistry(String defaultBaseUrl) {
    this.baseUrl = defaultBaseUrl;
  }

  @SuppressWarnings("unchecked")
  public T baseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public T namespace(String namespace) {
    this.namespace = namespace;
    return (T) this;
  }

  @Override
  public boolean isValid() {
    if (isBlank(namespace)) {
      return false;
    }

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
