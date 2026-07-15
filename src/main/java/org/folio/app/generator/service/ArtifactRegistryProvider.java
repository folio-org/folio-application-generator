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
import org.folio.app.generator.model.registry.artifact.EcrArtifactRegistry;
import org.folio.app.generator.model.registry.artifact.FolioNpmArtifactRegistry;
import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.folio.app.generator.service.parsers.StringArtifactRegistryParser;
import org.folio.app.generator.utils.PluginConfig;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ArtifactRegistryProvider {

  private static final List<ArtifactRegistryType> BE_ALLOWED_TYPES =
    List.of(ArtifactRegistryType.DOCKER_HUB, ArtifactRegistryType.AWS_ECR);
  private static final List<ArtifactRegistryType> UI_ALLOWED_TYPES =
    List.of(ArtifactRegistryType.FOLIO_NPM);
  private static final List<ArtifactRegistryType> UNIFIED_ALLOWED_TYPES =
    List.of(ArtifactRegistryType.DOCKER_HUB, ArtifactRegistryType.AWS_ECR, ArtifactRegistryType.FOLIO_NPM);

  private final StringArtifactRegistryParser artifactRegistryParser;

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
    var type = lowerCase(registry.getType());
    if (ArtifactRegistryType.DOCKER_HUB.getValue().equals(type)) {
      var dockerRegistry = new DockerHubArtifactRegistry().namespace(registry.getNamespace());
      if (isNotBlank(registry.getBaseUrl())) {
        dockerRegistry.baseUrl(registry.getBaseUrl());
      }
      return dockerRegistry;
    }

    if (ArtifactRegistryType.AWS_ECR.getValue().equals(type)) {
      var ecrRegistry = new EcrArtifactRegistry();
      if (isNotBlank(registry.getBaseUrl())) {
        ecrRegistry.baseUrl(registry.getBaseUrl());
      }
      if (isNotBlank(registry.getNamespace())) {
        ecrRegistry.namespace(registry.getNamespace());
      }
      return ecrRegistry;
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
      this.unifiedRegistries = new ArrayList<>(processCategory(
        config.getCmdArtifactRegistries(), config.getArtifactRegistries(), UNIFIED_ALLOWED_TYPES));
      this.beRegistries = new ArrayList<>(processCategory(
        config.getCmdBeArtifactRegistries(), config.getBeArtifactRegistries(), BE_ALLOWED_TYPES));
      this.uiRegistries = new ArrayList<>(processCategory(
        config.getCmdUiArtifactRegistries(), config.getUiArtifactRegistries(), UI_ALLOWED_TYPES));
      this.bePreReleaseRegistries = new ArrayList<>(processCategory(
        config.getCmdBePreReleaseArtifactRegistries(), config.getBePreReleaseArtifactRegistries(), BE_ALLOWED_TYPES));
      this.uiPreReleaseRegistries = new ArrayList<>(processCategory(
        config.getCmdUiPreReleaseArtifactRegistries(), config.getUiPreReleaseArtifactRegistries(), UI_ALLOWED_TYPES));

      applyDefaults();
    }

    private List<ArtifactRegistry> processCategory(String cmdRegistries,
                                                   List<ConfigArtifactRegistry> configRegistries,
                                                   List<ArtifactRegistryType> allowedTypes) {
      var cmdResult = processCommandLineRegistries(cmdRegistries, allowedTypes);
      invalidRegistries.addAll(cmdResult.invalidRegistries());

      var configResult = processConfigurationRegistries(configRegistries, allowedTypes);
      invalidRegistries.addAll(configResult.invalidRegistries());

      return merge(cmdResult.registries(), configResult.registries());
    }

    private RegistryProcessingResult processCommandLineRegistries(String registryString,
                                                                  List<ArtifactRegistryType> allowedTypes) {
      if (isBlank(registryString)) {
        return new RegistryProcessingResult(emptyList(), emptyList());
      }

      var result = new ArrayList<ArtifactRegistry>();
      var invalid = new ArrayList<String>();
      var registryStrings = registryString.split(",");

      for (var value : registryStrings) {
        var parsed = artifactRegistryParser.parse(value);
        if (parsed.isEmpty()) {
          invalid.add(value);
          continue;
        }
        var registry = parsed.get();
        if (!allowedTypes.contains(registry.getType()) || !registry.isValid()) {
          invalid.add(value);
        } else {
          result.add(registry);
        }
      }

      return new RegistryProcessingResult(result, invalid);
    }

    private RegistryProcessingResult processConfigurationRegistries(List<ConfigArtifactRegistry> registries,
                                                                    List<ArtifactRegistryType> allowedTypes) {
      if (registries == null || registries.isEmpty()) {
        return new RegistryProcessingResult(emptyList(), emptyList());
      }

      var allowedValues = allowedTypes.stream().map(ArtifactRegistryType::getValue).toList();
      var invalidRegs = new ArrayList<String>();
      var result = new ArrayList<ArtifactRegistry>();

      for (var configRegistry : registries) {
        if (!allowedValues.contains(lowerCase(configRegistry.getType()))) {
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
