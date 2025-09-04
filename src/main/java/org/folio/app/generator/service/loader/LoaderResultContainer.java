package org.folio.app.generator.service.loader;

import java.net.URL;
import java.util.Map;
import lombok.Data;

@Data
public class LoaderResultContainer {
  private URL sourceUrl;
  private Map<String, Object> moduleDescriptor;

  public LoaderResultContainer sourceUrl(URL sourceUrl) {
    this.sourceUrl = sourceUrl;
    return this;
  }

  public LoaderResultContainer moduleDescriptor(Map<String, Object> moduleDescriptor) {
    this.moduleDescriptor = moduleDescriptor;
    return this;
  }
}
