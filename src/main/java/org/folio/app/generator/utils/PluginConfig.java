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
  private final List<ConfigModuleRegistry> beRegistries;
  private final List<ConfigModuleRegistry> uiRegistries;

  private final String cmdRegistryString;
  private final String beCmdRegistryString;
  private final String uiCmdRegistryString;

  private final boolean moduleUrlsOnly;
  private final boolean overrideConfigRegistries;

  private final Region awsRegion;
  private final URI awsEndpointOverride;

  @Builder.Default
  private final int awsS3BatchSize = 1000;
}
