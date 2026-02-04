package org.folio.app.generator.model.registry;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.net.MalformedURLException;
import java.net.URL;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.folio.app.generator.model.types.RegistryType;

@Data
public class OkapiModuleRegistry implements ModuleRegistry {

  @Setter(AccessLevel.NONE)
  private RegistryType type = RegistryType.OKAPI;

  private String url;
  private String publicUrl;

  /**
   * Sets url field and returns {@link OkapiModuleRegistry}.
   *
   * @return modified {@link OkapiModuleRegistry} value
   */
  public OkapiModuleRegistry url(String url) {
    this.url = url;
    return this;
  }

  /**
   * Sets publicUrl field and returns {@link OkapiModuleRegistry}.
   *
   * @return modified {@link OkapiModuleRegistry} value
   */
  public OkapiModuleRegistry publicUrl(String publicUrl) {
    this.publicUrl = publicUrl;
    return this;
  }

  @Override
  public boolean isValid() {
    if (isBlank(url)) {
      return false;
    }

    try {
      new URL(url);
      return true;
    } catch (MalformedURLException e) {
      return false;
    }
  }

  @Override
  public OkapiModuleRegistry withGeneratedFields() {
    if (this.publicUrl == null) {
      this.publicUrl = this.url + "/_/proxy/modules/{id}";
    }

    return this;
  }

  @Override
  public String getRegistryIdentifier() {
    return url;
  }
}
