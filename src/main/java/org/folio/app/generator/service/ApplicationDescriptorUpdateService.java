package org.folio.app.generator.service;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.folio.app.generator.model.types.ModuleType.BE;
import static org.folio.app.generator.model.types.ModuleType.UI;
import static org.folio.app.generator.utils.PluginUtils.emptyIfNull;
import static org.folio.app.generator.utils.PluginUtils.splitModuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.ModulesLoadResult;
import org.folio.app.generator.model.PreReleaseFilter;
import org.folio.app.generator.model.UpdateConfig;
import org.folio.app.generator.model.UpdateResult;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.utils.PluginConfig;
import org.folio.app.generator.utils.SemverUtils;
import org.semver4j.Semver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationDescriptorUpdateService {

  private static final String LATEST_VERSION = "latest";
  private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");

  private final Log log;
  private final MavenProject mavenProject;
  private final PluginConfig pluginConfig;
  private final JsonProvider jsonProvider;
  private final ModuleDescriptorService moduleDescriptorService;
  private final ModuleVersionService moduleVersionService;

  public boolean update(ApplicationDescriptor application, String modulesIds, String uiModulesIds,
                        UpdateConfig config) throws MojoExecutionException {
    var moduleUpdates = parseModuleIdsToUpdate(modulesIds);
    var uiModuleUpdates = parseModuleIdsToUpdate(uiModulesIds);
    return update(application, moduleUpdates, uiModuleUpdates, config);
  }

  public boolean update(ApplicationDescriptor application, List<Dependency> modules, List<Dependency> uiModules,
                        UpdateConfig config) throws MojoExecutionException {
    var moduleUpdates = emptyIfNull(modules);
    var uiModuleUpdates = emptyIfNull(uiModules);

    var resolvedModules = resolveConstraints(moduleUpdates, BE);
    var resolvedUiModules = resolveConstraints(uiModuleUpdates, UI);

    var modulesResult = processModules(
      emptyIfNull(application.getModules()), resolvedModules, BE, config);
    var uiModulesResult = processModules(
      emptyIfNull(application.getUiModules()), resolvedUiModules, UI, config);

    if (!modulesResult.hasChanges() && !uiModulesResult.hasChanges()) {
      log.info("No module changes detected. Skipping descriptor update.");
      return false;
    }

    var modulesLoadResult = moduleDescriptorService.loadModules(BE, modulesResult.changedModules());
    var uiModulesLoadResult = moduleDescriptorService.loadModules(UI, uiModulesResult.changedModules());

    var baseVersion = config.isUseProjectVersion()
      ? mavenProject.getVersion()
      : application.getVersion();

    var version = resolveVersion(baseVersion, config.isNoVersionBump());

    application.setId(getId(application.getName(), version));
    application.setVersion(version);

    updateApplicationModules(application, modulesLoadResult, modulesResult, uiModulesLoadResult, uiModulesResult);
    updateApplicationDescriptors(application, modulesLoadResult, uiModulesLoadResult, modulesResult, uiModulesResult);

    var outputDir = mavenProject.getBuild().getDirectory();
    jsonProvider.writeApplication(application, outputDir);

    var updateResult = new UpdateResult(
      modulesResult.added(), modulesResult.upgraded(), modulesResult.downgraded(), modulesResult.removed(),
      uiModulesResult.added(), uiModulesResult.upgraded(), uiModulesResult.downgraded(), uiModulesResult.removed());
    jsonProvider.writeUpdateResult(updateResult, outputDir);
    return true;
  }

  private void updateApplicationModules(ApplicationDescriptor application,
                                        ModulesLoadResult modulesLoadResult,
                                        ModuleProcessResult modulesProcessResult,
                                        ModulesLoadResult uiModulesLoadResult,
                                        ModuleProcessResult uiModulesProcessResult) {
    if (pluginConfig.isModuleUrlsOnly()) {
      application.setModules(mergeModules(modulesLoadResult.artifacts(), modulesProcessResult.unchangedModules()));
      application.setUiModules(
        mergeModules(uiModulesLoadResult.artifacts(), uiModulesProcessResult.unchangedModules()));
    } else {
      application.setModules(mergeModules(clearUrls(modulesLoadResult.artifacts()),
        modulesProcessResult.unchangedModules()));
      application.setUiModules(mergeModules(clearUrls(uiModulesLoadResult.artifacts()),
        uiModulesProcessResult.unchangedModules()));
    }
  }

  private List<ModuleDefinition> mergeModules(List<ModuleDefinition> changedModules,
                                              List<ModuleDefinition> unchangedModules) {
    var result = new ArrayList<>(changedModules);
    result.addAll(unchangedModules);
    return result;
  }

  private void updateApplicationDescriptors(ApplicationDescriptor application,
                                            ModulesLoadResult modulesLoadResult,
                                            ModulesLoadResult uiModulesLoadResult,
                                            ModuleProcessResult modulesResult,
                                            ModuleProcessResult uiModulesResult) {
    if (pluginConfig.isModuleUrlsOnly()) {
      application.setModuleDescriptors(
        filterDescriptorsForUnchanged(application.getModuleDescriptors(), modulesResult));
      application.setUiModuleDescriptors(
        filterDescriptorsForUnchanged(application.getUiModuleDescriptors(), uiModulesResult));
    } else {
      application.setModuleDescriptors(
        mergeDescriptors(modulesLoadResult, application.getModuleDescriptors(), modulesResult));
      application.setUiModuleDescriptors(
        mergeDescriptors(uiModulesLoadResult, application.getUiModuleDescriptors(), uiModulesResult));
    }
  }

  private List<Map<String, Object>> filterDescriptorsForUnchanged(List<Map<String, Object>> existingDescriptors,
                                                                   ModuleProcessResult processResult) {
    if (existingDescriptors == null) {
      return List.of();
    }

    var unchangedModuleVersions = processResult.unchangedModules().stream()
      .collect(toMap(ModuleDefinition::getName, ModuleDefinition::getVersion));

    return existingDescriptors.stream()
      .filter(descriptor -> {
        var moduleId = getModuleId(descriptor);
        var moduleIdOpt = parseModuleId(moduleId);
        if (moduleIdOpt.isEmpty()) {
          log.warn("Skipping descriptor with invalid module ID: " + moduleId);
          return false;
        }

        var moduleName = moduleIdOpt.get().getName();
        var expectedVersion = unchangedModuleVersions.get(moduleName);
        if (expectedVersion == null) {
          return false;
        }

        var descriptorVersion = moduleIdOpt.get().getVersion();
        if (!expectedVersion.equals(descriptorVersion)) {
          log.warn(String.format("Descriptor version mismatch for '%s': expected %s, found %s",
            moduleName, expectedVersion, descriptorVersion));
          return false;
        }

        return true;
      })
      .toList();
  }

  private List<ModuleDefinition> clearUrls(List<ModuleDefinition> modules) {
    return emptyIfNull(modules).stream()
      .map(md -> new ModuleDefinition().id(md.getId()).name(md.getName()).version(md.getVersion()))
      .toList();
  }

  private ModuleProcessResult processModules(List<ModuleDefinition> existingModules,
                                             List<Dependency> updateModules,
                                             ModuleType type,
                                             UpdateConfig config) {
    var existingMap = existingModules.stream()
      .collect(toMap(ModuleDefinition::getName, Function.identity()));

    var updateModuleNames = updateModules.stream()
      .map(Dependency::getName)
      .collect(Collectors.toSet());

    List<ModuleDefinition> resultModules = new ArrayList<>();
    List<ModuleDefinition> changedModules = new ArrayList<>();
    List<ModuleDefinition> unchangedModules = new ArrayList<>();
    List<String> upgraded = new ArrayList<>();
    List<String> downgraded = new ArrayList<>();
    List<String> added = new ArrayList<>();
    List<String> removed = new ArrayList<>();
    List<String> unchanged = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    for (Dependency update : updateModules) {
      var moduleName = update.getName();
      var updateVersion = update.getVersion();
      var existingModule = existingMap.get(moduleName);

      if (existingModule == null) {
        if (config.isAllowAddModules()) {
          var newModule = toModuleDefinition(update);
          resultModules.add(newModule);
          changedModules.add(newModule);
          added.add(moduleName + "-" + updateVersion);
        } else {
          errors.add(String.format("Module '%s' not found in descriptor (use -DallowAddModules=true to add)",
            moduleName));
        }
      } else {
        var existingVersion = existingModule.getVersion();
        var comparison = compareVersions(existingVersion, updateVersion);

        if (comparison < 0) {
          var newModule = toModuleDefinition(update);
          resultModules.add(newModule);
          changedModules.add(newModule);
          upgraded.add(moduleName + " (" + existingVersion + " -> " + updateVersion + ")");
        } else if (comparison > 0) {
          if (config.isAllowDowngrade()) {
            var newModule = toModuleDefinition(update);
            resultModules.add(newModule);
            changedModules.add(newModule);
            downgraded.add(moduleName + " (" + existingVersion + " -> " + updateVersion + ")");
          } else {
            errors.add(String.format("Cannot downgrade '%s' from %s to %s (use -DallowDowngrade=true)",
              moduleName, existingVersion, updateVersion));
          }
        } else {
          resultModules.add(existingModule);
          unchangedModules.add(existingModule);
          unchanged.add(moduleName + "-" + updateVersion);
        }
      }
    }

    for (ModuleDefinition existing : existingModules) {
      if (!updateModuleNames.contains(existing.getName())) {
        if (config.isRemoveUnlistedModules()) {
          removed.add(existing.getId());
        } else {
          resultModules.add(existing);
          unchangedModules.add(existing);
          unchanged.add(existing.getId());
        }
      }
    }

    if (!errors.isEmpty()) {
      throw new IllegalArgumentException("Module update validation failed:\n  * " + String.join("\n  * ", errors));
    }

    logChanges(type, added, upgraded, downgraded, removed, unchanged);

    boolean hasChanges = !added.isEmpty() || !upgraded.isEmpty() || !downgraded.isEmpty() || !removed.isEmpty();
    boolean hasStructuralChanges = !added.isEmpty() || !removed.isEmpty();

    return new ModuleProcessResult(resultModules, changedModules, unchangedModules,
      hasChanges, hasStructuralChanges, updateModuleNames, added, upgraded, downgraded, removed);
  }

  private void logChanges(ModuleType type, List<String> added, List<String> upgraded,
                          List<String> downgraded, List<String> removed, List<String> unchanged) {
    var typeLabel = type == BE ? "BE" : "UI";

    if (!added.isEmpty()) {
      log.info(String.format("[%s] Added modules: %s", typeLabel, String.join(", ", added)));
    }
    if (!upgraded.isEmpty()) {
      log.info(String.format("[%s] Upgraded modules: %s", typeLabel, String.join(", ", upgraded)));
    }
    if (!downgraded.isEmpty()) {
      log.info(String.format("[%s] Downgraded modules: %s", typeLabel, String.join(", ", downgraded)));
    }
    if (!removed.isEmpty()) {
      log.info(String.format("[%s] Removed modules: %s", typeLabel, String.join(", ", removed)));
    }
    if (!unchanged.isEmpty()) {
      log.debug(String.format("[%s] Unchanged modules: %s", typeLabel, String.join(", ", unchanged)));
    }
  }

  private int compareVersions(String existingVersion, String updateVersion) {
    if (LATEST_VERSION.equals(updateVersion)) {
      return -1;
    }

    var existingSemver = SemverUtils.parse(existingVersion);
    var updateSemver = SemverUtils.parse(updateVersion);

    if (existingSemver == null || updateSemver == null) {
      log.warn(String.format("Unable to compare versions '%s' and '%s' - treating as unchanged",
        existingVersion, updateVersion));
      return 0;
    }

    return existingSemver.compareTo(updateSemver);
  }

  private List<Dependency> resolveConstraints(List<Dependency> dependencies, ModuleType type)
    throws MojoExecutionException {
    return moduleVersionService.resolveModulesConstraints(emptyIfNull(dependencies), type);
  }

  private String resolveVersion(String version, boolean noVersionBump) {
    var buildNumber = pluginConfig.getBuildNumber();
    var semver = Semver.parse(version);
    if (semver == null) {
      return version;
    }

    if (isNotBlank(buildNumber) && !semver.getPreRelease().isEmpty()) {
      return updateBuildNumber(semver, buildNumber);
    }

    if (noVersionBump) {
      return version;
    }
    return semver.withIncPatch().getVersion();
  }

  private String updateBuildNumber(Semver version, String buildNumber) {
    var preReleaseParts = new ArrayList<>(version.getPreRelease());
    var lastPart = preReleaseParts.get(preReleaseParts.size() - 1);

    if (DIGITS_PATTERN.matcher(lastPart).matches()) {
      preReleaseParts.set(preReleaseParts.size() - 1, buildNumber);
    } else {
      preReleaseParts.add(buildNumber);
    }

    return version.withPreRelease(String.join(".", preReleaseParts)).getVersion();
  }

  private List<Map<String, Object>> mergeDescriptors(ModulesLoadResult loadResult,
                                                     List<Map<String, Object>> existingDescriptors,
                                                     ModuleProcessResult processResult) {
    if (existingDescriptors == null) {
      return loadResult.descriptors();
    }

    Map<String, Map<String, Object>> newDescriptorsMap = loadResult.descriptors().stream()
      .map(descriptor -> parseModuleId(getModuleId(descriptor))
        .map(moduleId -> Pair.of(moduleId.getName(), descriptor)))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toMap(Pair::getKey, Pair::getValue, (d1, d2) -> d1));

    List<Map<String, Object>> mergedDescriptors = new ArrayList<>();

    for (Map<String, Object> existing : existingDescriptors) {
      var moduleIdOpt = parseModuleId(getModuleId(existing));
      if (moduleIdOpt.isEmpty()) {
        continue;
      }

      var moduleName = moduleIdOpt.get().getName();
      var newDescriptor = newDescriptorsMap.remove(moduleName);
      if (newDescriptor != null) {
        mergedDescriptors.add(newDescriptor);
      } else if (processResult.modules().stream().anyMatch(m -> m.getName().equals(moduleName))) {
        mergedDescriptors.add(existing);
      }
    }

    mergedDescriptors.addAll(newDescriptorsMap.values());

    return mergedDescriptors;
  }

  private ModuleDefinition toModuleDefinition(Dependency dependency) {
    var name = dependency.getName();
    var version = dependency.getVersion();
    return new ModuleDefinition().id(getId(name, version)).name(name).version(version);
  }

  private String getId(String name, String version) {
    return name + "-" + version;
  }

  private List<Dependency> parseModuleIdsToUpdate(String modules) {
    return isBlank(modules) ? emptyList() : stream(modules.split(","))
      .map(String::trim)
      .map(moduleId -> parseModuleId(moduleId)
        .orElseThrow(() -> new IllegalArgumentException("Invalid module id format: " + moduleId)))
      .toList();
  }

  private Optional<Dependency> parseModuleId(String moduleId) {
    if (moduleId.contains(":")) {
      var name = moduleId.substring(0, moduleId.indexOf(':'));
      var version = moduleId.substring(moduleId.indexOf(':') + 1);
      return Optional.of(new Dependency(name, version, PreReleaseFilter.TRUE));
    }
    return splitModuleId(moduleId);
  }

  private String getModuleId(Map<String, Object> descriptor) {
    return ofNullable(descriptor.get("id")).map(Object::toString).orElse("");
  }

  private record ModuleProcessResult(
    List<ModuleDefinition> modules,
    List<ModuleDefinition> changedModules,
    List<ModuleDefinition> unchangedModules,
    boolean hasChanges,
    boolean hasStructuralChanges,
    Set<String> updateModuleNames,
    List<String> added,
    List<String> upgraded,
    List<String> downgraded,
    List<String> removed
  ) {}
}
