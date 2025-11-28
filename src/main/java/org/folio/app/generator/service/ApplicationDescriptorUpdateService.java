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

  public void update(ApplicationDescriptor application, String modulesIds, String uiModulesIds,
                     UpdateConfig config) throws MojoExecutionException {
    var moduleUpdates = parseModuleIdsToUpdate(modulesIds);
    var uiModuleUpdates = parseModuleIdsToUpdate(uiModulesIds);
    update(application, moduleUpdates, uiModuleUpdates, config);
  }

  public void update(ApplicationDescriptor application, List<Dependency> modules, List<Dependency> uiModules,
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
      return;
    }

    var modulesLoadResult = moduleDescriptorService.loadModules(BE, modulesResult.modules());
    var uiModulesLoadResult = moduleDescriptorService.loadModules(UI, uiModulesResult.modules());

    var version = getUpdatedVersion(application.getVersion());

    application.setId(getId(application.getName(), version));
    application.setVersion(version);

    updateApplicationModules(application, modulesLoadResult, uiModulesLoadResult);
    updateApplicationDescriptors(application, modulesLoadResult, uiModulesLoadResult, modulesResult, uiModulesResult);

    jsonProvider.writeApplication(application, mavenProject.getBuild().getDirectory());
  }

  private void updateApplicationModules(ApplicationDescriptor application,
                                        ModulesLoadResult modulesLoadResult,
                                        ModulesLoadResult uiModulesLoadResult) {
    if (pluginConfig.isModuleUrlsOnly()) {
      application.setModules(modulesLoadResult.artifacts());
      application.setUiModules(uiModulesLoadResult.artifacts());
    } else {
      application.setModules(clearUrls(modulesLoadResult.artifacts()));
      application.setUiModules(clearUrls(uiModulesLoadResult.artifacts()));
    }
  }

  private void updateApplicationDescriptors(ApplicationDescriptor application,
                                            ModulesLoadResult modulesLoadResult,
                                            ModulesLoadResult uiModulesLoadResult,
                                            ModuleProcessResult modulesResult,
                                            ModuleProcessResult uiModulesResult) {
    if (pluginConfig.isModuleUrlsOnly()) {
      application.setModuleDescriptors(List.of());
      application.setUiModuleDescriptors(List.of());
    } else {
      application.setModuleDescriptors(
        mergeDescriptors(modulesLoadResult, application.getModuleDescriptors(), modulesResult));
      application.setUiModuleDescriptors(
        mergeDescriptors(uiModulesLoadResult, application.getUiModuleDescriptors(), uiModulesResult));
    }
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
          resultModules.add(toModuleDefinition(update));
          added.add(moduleName + "-" + updateVersion);
        } else {
          errors.add(String.format("Module '%s' not found in descriptor (use -DallowAddModules=true to add)",
            moduleName));
        }
      } else {
        var existingVersion = existingModule.getVersion();
        var comparison = compareVersions(existingVersion, updateVersion);

        if (comparison < 0) {
          resultModules.add(toModuleDefinition(update));
          upgraded.add(moduleName + " (" + existingVersion + " -> " + updateVersion + ")");
        } else if (comparison > 0) {
          if (config.isAllowDowngrade()) {
            resultModules.add(toModuleDefinition(update));
            downgraded.add(moduleName + " (" + existingVersion + " -> " + updateVersion + ")");
          } else {
            errors.add(String.format("Cannot downgrade '%s' from %s to %s (use -DallowDowngrade=true)",
              moduleName, existingVersion, updateVersion));
          }
        } else {
          resultModules.add(existingModule);
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

    return new ModuleProcessResult(resultModules, hasChanges, hasStructuralChanges, updateModuleNames);
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
      return existingVersion.compareTo(updateVersion);
    }

    return existingSemver.compareTo(updateSemver);
  }

  private List<Dependency> resolveConstraints(List<Dependency> dependencies, ModuleType type)
    throws MojoExecutionException {
    return moduleVersionService.resolveModulesConstraints(emptyIfNull(dependencies), type);
  }

  private String getUpdatedVersion(String version) {
    var buildNumber = pluginConfig.getBuildNumber();
    var semver = Semver.parse(version);
    if (semver == null) {
      return version;
    }

    if (isNotBlank(buildNumber) && !semver.getPreRelease().isEmpty()) {
      return updateBuildNumber(semver, buildNumber);
    } else {
      return semver.withIncPatch().getVersion();
    }
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
      } else if (!processResult.updateModuleNames().contains(moduleName)
        || processResult.modules().stream().anyMatch(m -> m.getName().equals(moduleName))) {
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
    if (moduleId.contains(":" + LATEST_VERSION)) {
      var name = moduleId.substring(0, moduleId.indexOf(':'));
      return Optional.of(new Dependency(name, LATEST_VERSION, PreReleaseFilter.TRUE));
    }
    return splitModuleId(moduleId);
  }

  private String getModuleId(Map<String, Object> descriptor) {
    return ofNullable(descriptor.get("id")).map(Object::toString).orElse("");
  }

  private record ModuleProcessResult(
    List<ModuleDefinition> modules,
    boolean hasChanges,
    boolean hasStructuralChanges,
    Set<String> updateModuleNames
  ) {}
}
