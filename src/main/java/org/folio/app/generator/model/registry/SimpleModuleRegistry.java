package org.folio.app.generator.model.registry;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.folio.app.generator.model.types.RegistryType;

@Data
public class SimpleModuleRegistry implements ModuleRegistry {

  @Setter(AccessLevel.NONE)
  private RegistryType type = RegistryType.SIMPLE;

  private String url;
  private String publicUrl;
  private Map<String, String> headers = new LinkedHashMap<>();
  private boolean fallback;

  /**
   * Sets url field and returns {@link SimpleModuleRegistry}.
   *
   * @return modified {@link SimpleModuleRegistry} value
   */
  public SimpleModuleRegistry url(String url) {
    this.url = url;
    return this;
  }

  /**
   * Sets headers field and returns {@link SimpleModuleRegistry}.
   *
   * @return modified {@link SimpleModuleRegistry} value
   */
  public SimpleModuleRegistry headers(Map<String, String> headers) {
    this.headers = headers;
    return this;
  }

  /**
   * Sets publicUrl field and returns {@link SimpleModuleRegistry}.
   *
   * @return modified {@link SimpleModuleRegistry} value
   */
  public SimpleModuleRegistry publicUrl(String publicUrl) {
    this.publicUrl = publicUrl;
    return this;
  }

  @Override
  public boolean isValid() {
    try {
      if (!isBlank(url)) {
        new URI(url).toURL();
        return true;
      }
    } catch (MalformedURLException | URISyntaxException e) {
      // This exception is used to determine the validity and no error needs to be thrown.
    }

    return false;
  }

  @Override
  public SimpleModuleRegistry withGeneratedFields() {
    if (this.publicUrl == null) {
      this.publicUrl = this.url + "/{id}";
    }

    return this;
  }

  @Override
  public String getRegistryIdentifier() {
    return url;
  }
}
