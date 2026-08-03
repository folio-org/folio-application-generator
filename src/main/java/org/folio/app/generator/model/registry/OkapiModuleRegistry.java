package org.folio.app.generator.model.registry;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
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
  private Map<String, String> headers = new LinkedHashMap<>();
  private boolean fallback;

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
   * Sets headers field and returns {@link OkapiModuleRegistry}.
   *
   * @return modified {@link OkapiModuleRegistry} value
   */
  public OkapiModuleRegistry headers(Map<String, String> headers) {
    this.headers = headers;
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
