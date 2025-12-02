package org.folio.app.generator.utils;

import java.net.URI;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.folio.app.generator.model.registry.ConfigModuleRegistry;
import org.folio.app.generator.model.registry.artifact.ConfigArtifactRegistry;
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

  private final boolean validateArtifacts;

  private final List<ConfigArtifactRegistry> artifactRegistries;
  private final List<ConfigArtifactRegistry> beArtifactRegistries;
  private final List<ConfigArtifactRegistry> uiArtifactRegistries;
  private final List<ConfigArtifactRegistry> bePreReleaseArtifactRegistries;
  private final List<ConfigArtifactRegistry> uiPreReleaseArtifactRegistries;

  private final String cmdArtifactRegistries;
  private final String cmdBeArtifactRegistries;
  private final String cmdUiArtifactRegistries;
  private final String cmdBePreReleaseArtifactRegistries;
  private final String cmdUiPreReleaseArtifactRegistries;
}
