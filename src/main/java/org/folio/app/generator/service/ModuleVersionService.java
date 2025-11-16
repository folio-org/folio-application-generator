package org.folio.app.generator.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.PreReleaseFilter;
import org.folio.app.generator.model.registry.ModuleRegistries;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.service.resolver.ModuleVersionResolverFacade;
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

  /**
   * Resolves version constraints to exact versions for a list of dependencies.
   * For each dependency with a constraint, collects matching versions from ALL registries
   * and selects the greatest version.
   *
   * @param dependencies list of dependencies (may contain constraints)
   * @param type         the module type (BE or UI)
   * @return list of dependencies with exact versions
   * @throws MojoExecutionException if any constraint cannot be resolved
   */
  public List<Dependency> resolveModulesConstraints(List<Dependency> dependencies, ModuleType type)
        throws MojoExecutionException {

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
   * @throws MojoExecutionException if no matching version is found
   */
  private Dependency resolveModuleConstraints(Dependency dependency, ModuleType type, List<ModuleRegistry> registries)
        throws MojoExecutionException {

    var moduleName = dependency.getName();
    var versionConstraint = dependency.getVersion();

    if (SemverUtils.parse(versionConstraint) != null) {
      return dependency;
    }

    List<VersionCandidate> allMatchingVersions = new ArrayList<>();

    for (var registry : registries) {
      try {
        var matchingVersion = getMatchingVersionFromRegistry(registry, dependency, type);
        matchingVersion.ifPresent(allMatchingVersions::add);
      } catch (Exception e) {
        log.warn(String.format("Failed to resolve constraint '%s' for module '%s' from %s",
          versionConstraint, moduleName, registry.getClass().getSimpleName()), e);
      }
    }

    if (allMatchingVersions.isEmpty()) {
      throw new MojoExecutionException(String.format(
        "No version matching constraint '%s' found for %s module '%s' in any registry",
        versionConstraint, type, moduleName));
    }

    var greatestVersion = allMatchingVersions.stream()
      .max(Comparator.comparing(VersionCandidate::semver))
      .orElseThrow();

    var resolvedVersionString = greatestVersion.original();
    log.info(String.format("Resolved %s module '%s' version constraint '%s' to '%s'",
      type, moduleName, versionConstraint, resolvedVersionString));

    return new Dependency(moduleName, resolvedVersionString, dependency.getPreRelease());
  }

  /**
   * Get a max matching version for a dependency from a specific registry.
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
    boolean includePreRelease =
      dependency.getPreRelease() == PreReleaseFilter.TRUE
        || dependency.getPreRelease() == PreReleaseFilter.ONLY;

    var rangeList = RangesListFactory.create(SemverUtils.normalizeVersion(constraint), includePreRelease);

    if (rangeList.get().isEmpty()) {
      log.warn(String.format("Invalid version constraint '%s' for module '%s'", constraint, moduleName));
      return Optional.empty();
    }

    return availableVersions.stream()
      .map(original -> new VersionCandidate(original, SemverUtils.parse(original)))
      .filter(vc -> vc.semver() != null)
      .filter(vc -> rangeList.isSatisfiedBy(vc.semver()))
      .max(Comparator.comparing(VersionCandidate::semver));
  }

  /**
   * Holds both the original version string and the parsed Semver object.
   * The original string is needed because SemverUtils normalizes UI module versions.
   */
  private record VersionCandidate(String original, Semver semver) {}
}
