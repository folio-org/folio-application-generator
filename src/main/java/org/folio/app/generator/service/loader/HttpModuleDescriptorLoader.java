package org.folio.app.generator.service.loader;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.utils.HttpRetryHelper;
import org.folio.app.generator.utils.JsonConverter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public abstract class HttpModuleDescriptorLoader implements ModuleDescriptorLoader {

  protected final HttpClient httpClient;
  protected final Log log;
  protected final JsonConverter jsonConverter;

  @SneakyThrows
  protected HttpResponse<InputStream> retryLoad(HttpRequest request) {
    return HttpRetryHelper.sendWithRetry(httpClient, log, request);
  }

  protected static String cleanUrl(String url) {
    return HttpRetryHelper.cleanUrl(url);
  }
}
