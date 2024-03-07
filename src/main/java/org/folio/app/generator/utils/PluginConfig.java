package org.folio.app.generator.utils;

import java.net.URI;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.folio.app.generator.model.registry.ConfigModuleRegistry;
import software.amazon.awssdk.regions.Region;

@Data
@Builder
public class PluginConfig {

  private final String buildNumber;
  private final List<ConfigModuleRegistry> registries;

  private final String cmdRegistryString;
  private final boolean useModuleDescriptorsUrls;
  private final boolean overrideConfigRegistries;

  private final Region awsRegion;
  private final URI awsEndpointOverride;

  @Builder.Default
  private final int awsS3BatchSize = 1000;
}
