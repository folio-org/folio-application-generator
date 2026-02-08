package org.folio.app.generator.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.ErrorDetail;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.registry.ModuleRegistries;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.service.artifact.existence.ArtifactExistenceCheckerFacade;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.service.resolver.ModuleVersionResolverFacade;
import org.folio.app.generator.utils.PluginConfig;
import org.folio.app.generator.utils.SemverUtils;
import org.semver4j.RangesListFactory;
import org.semver4j.Semver;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModuleVersionService {

  private final Log log;
  private final ModuleRegistries moduleRegistries;
  private final ModuleVersionResolverFacade moduleVersionResolverFacade;
  private final PluginConfig pluginConfig;
  private final ArtifactExistenceCheckerFacade artifactExistenceCheckerFacade;
  private final ArtifactRegistryProvider artifactRegistryProvider;

  /**
   * Resolves version constraints to exact versions for a list of dependencies.
   * For each dependency with a constraint, collects matching versions from ALL registries
   * and selects the greatest version.
   *
   * @param dependencies list of dependencies (may contain constraints)
   * @param type         the module type (BE or UI)
   * @return list of dependencies with exact versions
   * @throws ApplicationGeneratorException if any constraint cannot be resolved
   */
  public List<Dependency> resolveModulesConstraints(List<Dependency> dependencies, ModuleType type)
    throws ApplicationGeneratorException {

    var registries = moduleRegistries.getRegistries(type);

    if (registries.isEmpty()) {
      log.warn("Module registries are empty for type: " + type.name());
    }

    List<Dependency> resolved = new ArrayList<>();
    for (Dependency dependency : dependencies) {
      resolved.add(resolveModuleConstraints(dependency, type, registries));
    }

    return resolved;
  }

  /**
   * Resolves a single dependency's version constraint to an exact version.
   * Collects matching versions from ALL registries and selects the greatest one.
   *
   * @param dependency the dependency with version constraint
   * @param type       the module type (BE or UI)
   * @param registries list of module registries to query
   * @return dependency with resolved exact version
   * @throws ApplicationGeneratorException if no matching version is found
   */
  private Dependency resolveModuleConstraints(Dependency dependency, ModuleType type, List<ModuleRegistry> registries)
    throws ApplicationGeneratorException {

    var moduleName = dependency.getName();
    var versionConstraint = dependency.getVersion();

    if (SemverUtils.parse(versionConstraint) != null) {
      return dependency;
    }

    List<VersionCandidate> allMatchingVersions = new ArrayList<>();
    int registryErrorCount = 0;
    Exception lastException = null;

    for (var registry : registries) {
      try {
        var matchingVersion = getMatchingVersionFromRegistry(registry, dependency, type);
        matchingVersion.ifPresent(allMatchingVersions::add);
      } catch (Exception e) {
        registryErrorCount++;
        lastException = e;
        log.warn(String.format("Failed to resolve constraint '%s' for module '%s' from %s",
          versionConstraint, moduleName, registry.getClass().getSimpleName()), e);
      }
    }

    var fallbackRegistries = moduleRegistries.getFallbackRegistries(type);
    int totalRegistryCount = registries.size() + fallbackRegistries.size();

    if (allMatchingVersions.isEmpty() && !fallbackRegistries.isEmpty()) {
      log.info(String.format("Trying fallback registries for '%s' constraint '%s'", moduleName, versionConstraint));

      for (var registry : fallbackRegistries) {
        try {
          var matchingVersion = getMatchingVersionFromRegistry(registry, dependency, type);
          if (matchingVersion.isPresent()) {
            log.info(String.format("Found '%s' in fallback registry: %s",
              moduleName, registry.getRegistryIdentifier()));
            allMatchingVersions.add(matchingVersion.get());
          }
        } catch (Exception e) {
          registryErrorCount++;
          lastException = e;
          log.warn(String.format("Failed to resolve constraint '%s' for module '%s' from fallback %s",
            versionConstraint, moduleName, registry.getClass().getSimpleName()), e);
        }
      }
    }

    if (allMatchingVersions.isEmpty()) {
      var allRegistriesFailed = registryErrorCount == totalRegistryCount && totalRegistryCount > 0;

      if (allRegistriesFailed) {
        var exMsg = lastException.getMessage();
        var errorMsg = exMsg != null ? exMsg : lastException.getClass().getSimpleName();
        var message = String.format("Infrastructure error while resolving %s module '%s': %s",
          type, moduleName, errorMsg);
        var errorDetail = ErrorDetail.infrastructureError(null, errorMsg);
        throw new ApplicationGeneratorException(message, ErrorCategory.INFRASTRUCTURE, errorDetail, lastException);
      }

      var message = String.format("No version matching constraint '%s' found for %s module '%s' in any registry",
        versionConstraint, type, moduleName);
      var errorDetail = ErrorDetail.moduleNotFound(moduleName, versionConstraint, versionConstraint);
      throw new ApplicationGeneratorException(message, ErrorCategory.MODULE_NOT_FOUND, errorDetail);
    }

    var greatestVersion = allMatchingVersions.stream()
      .max(Comparator.comparing(VersionCandidate::semver))
      .orElseThrow();

    var resolvedVersionString = greatestVersion.original();
    var registryIdentifier = greatestVersion.registry().getRegistryIdentifier();
    log.info(String.format("Resolved %s module '%s' version constraint '%s' to '%s' from %s",
      type, moduleName, versionConstraint, resolvedVersionString, registryIdentifier));

    return new Dependency(moduleName, resolvedVersionString, dependency.getPreRelease());
  }

  /**
   * Get a max matching version for a dependency from a specific registry.
   * If artifact validation is enabled, iterates through matching versions (highest first)
   * and returns the first version with an existing artifact.
   *
   * @param registry   the module registry to query
   * @param dependency the dependency with version constraint
   * @param type       the module type (BE or UI)
   * @return resolved version if found, empty otherwise
   */
  private Optional<VersionCandidate> getMatchingVersionFromRegistry(ModuleRegistry registry, Dependency dependency,
                                                          ModuleType type) {
    var availableVersionsOpt = moduleVersionResolverFacade.getAvailableVersions(registry, dependency, type);
    if (availableVersionsOpt.isEmpty() || availableVersionsOpt.get().isEmpty()) {
      return Optional.empty();
    }

    var availableVersions = availableVersionsOpt.get();
    var moduleName = dependency.getName();
    var constraint = dependency.getVersion();
    var preReleaseFilter = dependency.getPreRelease();
    boolean includePreRelease = preReleaseFilter == null || preReleaseFilter.isPreRelease();

    var rangeList = RangesListFactory.create(SemverUtils.normalizeVersion(constraint), includePreRelease);

    if (rangeList.get().isEmpty()) {
      log.warn(String.format("Invalid version constraint '%s' for module '%s'", constraint, moduleName));
      return Optional.empty();
    }

    var matchingVersions = availableVersions.stream()
      .map(original -> new VersionCandidate(original, SemverUtils.parse(original), registry))
      .filter(vc -> vc.semver() != null)
      .filter(vc -> rangeList.isSatisfiedBy(vc.semver()))
      .sorted(Comparator.comparing(VersionCandidate::semver).reversed())
      .toList();

    if (!pluginConfig.isValidateArtifacts()) {
      return matchingVersions.stream().findFirst();
    }

    for (var candidate : matchingVersions) {
      if (artifactExists(moduleName, candidate, type)) {
        return Optional.of(candidate);
      }
      log.debug(String.format("Artifact not found for %s-%s, trying next version...",
        moduleName, candidate.original()));
    }
    return Optional.empty();
  }

  private boolean artifactExists(String moduleName, VersionCandidate candidate, ModuleType type) {
    var registries = artifactRegistryProvider.getArtifactRegistries(pluginConfig);
    var isPreRelease = SemverUtils.isPreRelease(candidate.original());
    var artifactRegistries = registries.getRegistries(type, isPreRelease);
    var module = new ModuleDefinition().name(moduleName).version(candidate.original());

    for (var registry : artifactRegistries) {
      if (artifactExistenceCheckerFacade.exists(module, registry, type)) {
        log.info(String.format("Artifact found: %s-%s", moduleName, candidate.original()));
        return true;
      }
    }
    log.warn(String.format("Artifact not found: %s-%s", moduleName, candidate.original()));
    return false;
  }

  /**
   * Holds both the original version string, parsed Semver object, and source registry.
   * The original string is needed because SemverUtils normalizes UI module versions.
   */
  private record VersionCandidate(String original, Semver semver, ModuleRegistry registry) {}
}
