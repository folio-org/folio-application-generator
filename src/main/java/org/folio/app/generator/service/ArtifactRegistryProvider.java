package org.folio.app.generator.service;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.folio.app.generator.utils.PluginUtils.collectToBulletedList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistries;
import org.folio.app.generator.model.registry.artifact.ArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.ConfigArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.DockerHubArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.FolioNpmArtifactRegistry;
import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.folio.app.generator.service.parsers.StringArtifactRegistryParser;
import org.folio.app.generator.utils.PluginConfig;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ArtifactRegistryProvider {

  private final StringArtifactRegistryParser artifactRegistryParser;
  private final List<String> supportedDockerTypes = List.of(ArtifactRegistryType.DOCKER_HUB.getValue());
  private final List<String> supportedNpmTypes = List.of(ArtifactRegistryType.FOLIO_NPM.getValue());

  public ArtifactRegistries getArtifactRegistries(PluginConfig config) {
    var processor = new ArtifactRegistryProcessor(config);
    handleInvalidRegistries(processor.getInvalidRegistries());
    return new ArtifactRegistries(
      processor.getBeRegistries(),
      processor.getUiRegistries(),
      processor.getBePreReleaseRegistries(),
      processor.getUiPreReleaseRegistries(),
      processor.getUnifiedRegistries()
    );
  }

  private static void handleInvalidRegistries(List<String> invalidRegistries) {
    if (!invalidRegistries.isEmpty()) {
      var invalidRegistryString = collectToBulletedList(invalidRegistries);
      throw new IllegalArgumentException("Invalid artifact registries found: " + invalidRegistryString);
    }
  }

  private static ArtifactRegistry toArtifactRegistry(ConfigArtifactRegistry registry) {
    if (ArtifactRegistryType.DOCKER_HUB.getValue().equals(lowerCase(registry.getType()))) {
      var dockerRegistry = new DockerHubArtifactRegistry().namespace(registry.getNamespace());
      if (isNotBlank(registry.getBaseUrl())) {
        dockerRegistry.baseUrl(registry.getBaseUrl());
      }
      return dockerRegistry;
    }

    var npmRegistry = new FolioNpmArtifactRegistry().namespace(registry.getNamespace());
    if (isNotBlank(registry.getBaseUrl())) {
      npmRegistry.baseUrl(registry.getBaseUrl());
    }
    return npmRegistry;
  }

  @SafeVarargs
  private static <T> List<T> merge(Collection<T>... collections) {
    return Arrays.stream(collections)
      .flatMap(Collection::stream)
      .toList();
  }

  private final class ArtifactRegistryProcessor {

    @Getter private final List<String> invalidRegistries;
    @Getter private final List<ArtifactRegistry> unifiedRegistries;
    @Getter private final List<ArtifactRegistry> beRegistries;
    @Getter private final List<ArtifactRegistry> uiRegistries;
    @Getter private final List<ArtifactRegistry> bePreReleaseRegistries;
    @Getter private final List<ArtifactRegistry> uiPreReleaseRegistries;

    ArtifactRegistryProcessor(PluginConfig config) {
      this.invalidRegistries = new ArrayList<>();
      this.unifiedRegistries = new ArrayList<>(processUnifiedRegistries(config));
      this.beRegistries = new ArrayList<>(processBeRegistries(config));
      this.uiRegistries = new ArrayList<>(processUiRegistries(config));
      this.bePreReleaseRegistries = new ArrayList<>(processBePreReleaseRegistries(config));
      this.uiPreReleaseRegistries = new ArrayList<>(processUiPreReleaseRegistries(config));

      applyDefaults();
    }

    private List<ArtifactRegistry> processUnifiedRegistries(PluginConfig config) {
      var cmdResult = processUnifiedCmdRegistries(config.getCmdArtifactRegistries());
      invalidRegistries.addAll(cmdResult.invalidRegistries());

      var configResult = processUnifiedConfigRegistries(config.getArtifactRegistries());
      invalidRegistries.addAll(configResult.invalidRegistries());

      return merge(cmdResult.registries(), configResult.registries());
    }

    private RegistryProcessingResult processUnifiedCmdRegistries(String cmdRegistries) {
      if (isBlank(cmdRegistries)) {
        return new RegistryProcessingResult(emptyList(), emptyList());
      }

      var result = new ArrayList<ArtifactRegistry>();
      var registryStrings = cmdRegistries.split(",");

      for (var value : registryStrings) {
        var dockerRegistry = artifactRegistryParser.parse(value, ArtifactRegistryType.DOCKER_HUB);
        dockerRegistry.ifPresent(result::add);
      }

      return new RegistryProcessingResult(result, emptyList());
    }

    private RegistryProcessingResult processUnifiedConfigRegistries(List<ConfigArtifactRegistry> registries) {
      if (registries == null || registries.isEmpty()) {
        return new RegistryProcessingResult(emptyList(), emptyList());
      }

      var allSupportedTypes = new ArrayList<>(supportedDockerTypes);
      allSupportedTypes.addAll(supportedNpmTypes);

      return processConfigurationRegistries(registries, allSupportedTypes);
    }

    private RegistryProcessingResult processCommandLineRegistries(String registryString, ArtifactRegistryType type) {
      if (isBlank(registryString)) {
        return new RegistryProcessingResult(emptyList(), emptyList());
      }

      var result = new ArrayList<ArtifactRegistry>();
      var registryStrings = registryString.split(",");

      for (var value : registryStrings) {
        var registry = artifactRegistryParser.parse(value, type);
        registry.ifPresent(result::add);
      }

      return new RegistryProcessingResult(result, emptyList());
    }

    private RegistryProcessingResult processConfigurationRegistries(List<ConfigArtifactRegistry> registries,
                                                                    List<String> supportedTypes) {
      if (registries == null || registries.isEmpty()) {
        return new RegistryProcessingResult(emptyList(), emptyList());
      }

      var invalidRegs = new ArrayList<String>();
      var result = new ArrayList<ArtifactRegistry>();

      for (var configRegistry : registries) {
        if (!supportedTypes.contains(lowerCase(configRegistry.getType()))) {
          invalidRegs.add(configRegistry.toString());
          continue;
        }

        var registry = toArtifactRegistry(configRegistry);
        if (!registry.isValid()) {
          invalidRegs.add(configRegistry.toString());
        } else {
          result.add(registry);
        }
      }

      return new RegistryProcessingResult(result, invalidRegs);
    }

    private List<ArtifactRegistry> processBeRegistries(PluginConfig config) {
      var cmdResult = processCommandLineRegistries(
        config.getCmdBeArtifactRegistries(), ArtifactRegistryType.DOCKER_HUB);
      invalidRegistries.addAll(cmdResult.invalidRegistries());

      var configResult = processConfigurationRegistries(config.getBeArtifactRegistries(), supportedDockerTypes);
      invalidRegistries.addAll(configResult.invalidRegistries());

      return merge(cmdResult.registries(), configResult.registries());
    }

    private List<ArtifactRegistry> processUiRegistries(PluginConfig config) {
      var cmdResult = processCommandLineRegistries(
        config.getCmdUiArtifactRegistries(), ArtifactRegistryType.FOLIO_NPM);
      invalidRegistries.addAll(cmdResult.invalidRegistries());

      var configResult = processConfigurationRegistries(config.getUiArtifactRegistries(), supportedNpmTypes);
      invalidRegistries.addAll(configResult.invalidRegistries());

      return merge(cmdResult.registries(), configResult.registries());
    }

    private List<ArtifactRegistry> processBePreReleaseRegistries(PluginConfig config) {
      var cmdResult = processCommandLineRegistries(
        config.getCmdBePreReleaseArtifactRegistries(), ArtifactRegistryType.DOCKER_HUB);
      invalidRegistries.addAll(cmdResult.invalidRegistries());

      var configResult = processConfigurationRegistries(
        config.getBePreReleaseArtifactRegistries(), supportedDockerTypes);
      invalidRegistries.addAll(configResult.invalidRegistries());

      return merge(cmdResult.registries(), configResult.registries());
    }

    private List<ArtifactRegistry> processUiPreReleaseRegistries(PluginConfig config) {
      var cmdResult = processCommandLineRegistries(
        config.getCmdUiPreReleaseArtifactRegistries(), ArtifactRegistryType.FOLIO_NPM);
      invalidRegistries.addAll(cmdResult.invalidRegistries());

      var configResult = processConfigurationRegistries(
        config.getUiPreReleaseArtifactRegistries(), supportedNpmTypes);
      invalidRegistries.addAll(configResult.invalidRegistries());

      return merge(cmdResult.registries(), configResult.registries());
    }

    private void applyDefaults() {
      var hasBeConfig = !beRegistries.isEmpty() || !bePreReleaseRegistries.isEmpty();
      var hasUiConfig = !uiRegistries.isEmpty() || !uiPreReleaseRegistries.isEmpty();
      var hasUnifiedConfig = !unifiedRegistries.isEmpty();

      if (!hasBeConfig && !hasUnifiedConfig) {
        beRegistries.addAll(getDefaultBeReleaseRegistries());
        bePreReleaseRegistries.addAll(getDefaultBePreReleaseRegistries());
      }

      if (!hasUiConfig && !hasUnifiedConfig) {
        uiRegistries.addAll(getDefaultUiReleaseRegistries());
        uiPreReleaseRegistries.addAll(getDefaultUiPreReleaseRegistries());
      }
    }

    private List<ArtifactRegistry> getDefaultBeReleaseRegistries() {
      return List.of(new DockerHubArtifactRegistry().namespace("folioorg"));
    }

    private List<ArtifactRegistry> getDefaultBePreReleaseRegistries() {
      return List.of(new DockerHubArtifactRegistry().namespace("folioci"));
    }

    private List<ArtifactRegistry> getDefaultUiReleaseRegistries() {
      return List.of(new FolioNpmArtifactRegistry().namespace("npm-folio"));
    }

    private List<ArtifactRegistry> getDefaultUiPreReleaseRegistries() {
      return List.of(new FolioNpmArtifactRegistry().namespace("npm-folioci"));
    }
  }

  private record RegistryProcessingResult(List<ArtifactRegistry> registries, List<String> invalidRegistries) {}
}
