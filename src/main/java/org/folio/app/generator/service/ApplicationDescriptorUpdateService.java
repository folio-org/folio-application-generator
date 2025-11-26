package org.folio.app.generator.service;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.folio.app.generator.model.types.ModuleType.BE;
import static org.folio.app.generator.model.types.ModuleType.UI;
import static org.folio.app.generator.utils.PluginUtils.collectToBulletedList;
import static org.folio.app.generator.utils.PluginUtils.emptyIfNull;
import static org.folio.app.generator.utils.PluginUtils.splitModuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.model.ModulesLoadResult;
import org.folio.app.generator.model.PreReleaseFilter;
import org.folio.app.generator.model.types.ModuleType;
import org.folio.app.generator.utils.PluginConfig;
import org.folio.app.generator.utils.SemverUtils;
import org.semver4j.Semver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationDescriptorUpdateService {

  private static final String LATEST_VERSION = "latest";
  private final MavenProject mavenProject;
  private final PluginConfig pluginConfig;
  private final JsonProvider jsonProvider;
  private final ModuleDescriptorService moduleDescriptorService;
  private final ModuleVersionService moduleVersionService;

  /**
   * Update application descriptor with new versions of modulesIds.
   *
   * @param application  - old application descriptor {@link ApplicationDescriptor} object to update
   * @param modulesIds   - input BE module ids to update
   * @param uiModulesIds - input UI module ids to update
   * @throws MojoExecutionException if application description was failed to be updated
   */
  public void update(ApplicationDescriptor application, String modulesIds, String uiModulesIds)
    throws MojoExecutionException {
    if (isBlank(modulesIds) && isBlank(uiModulesIds)) {
      throw new IllegalArgumentException(
        "Update failed: both -Dmodules and -DuiModules parameters are missing or empty");
    }

    var moduleNameVersion = parseModuleIdsToUpdate(modulesIds);
    var uiModuleNameVersion = parseModuleIdsToUpdate(uiModulesIds);

    var resolvedModules = resolveConstraints(moduleNameVersion, BE);
    var resolvedUiModules = resolveConstraints(uiModuleNameVersion, UI);

    validateModules(resolvedModules, application.getModules());
    validateModules(resolvedUiModules, application.getUiModules());

    var modules = convertToArtifacts(resolvedModules);
    var uiModules = convertToArtifacts(resolvedUiModules);

    var modulesLoadResult = moduleDescriptorService.loadModules(BE, modules);
    var uiModulesLoadResult = moduleDescriptorService.loadModules(UI, uiModules);

    var version = getUpdatedVersion(application.getVersion());
    application.setId(getId(application.getName(), version));
    application.setVersion(version);
    application.setModules(updateModules(application.getModules(), modulesLoadResult));
    application.setUiModules(updateModules(application.getUiModules(), uiModulesLoadResult));

    application.setModuleDescriptors(updateDescriptors(modulesLoadResult, application.getModuleDescriptors()));
    application.setUiModuleDescriptors(updateDescriptors(uiModulesLoadResult, application.getUiModuleDescriptors()));

    jsonProvider.writeApplication(application, mavenProject.getBuild().getDirectory());
  }

  private void validateModules(List<Dependency> modulesToUpdate, List<ModuleDefinition> modulesList) {
    if (modulesToUpdate.isEmpty()) {
      return;
    }

    if (modulesList == null) {
      throw new IllegalArgumentException(
        "Cannot validate modules: modules list is null in application descriptor");
    }

    var existingModules = getModuleListIds(modulesList);
    validate(existingModules, modulesToUpdate);
  }

  private List<Dependency> getModuleListIds(List<ModuleDefinition> modules) {
    return modules.stream()
      .map(md -> new Dependency(md.getName(), md.getVersion(), PreReleaseFilter.fromVersion(md.getVersion())))
      .toList();
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

    if (lastPart.matches("\\d+")) {
      preReleaseParts.set(preReleaseParts.size() - 1, buildNumber);
    } else {
      preReleaseParts.add(buildNumber);
    }

    return version.withPreRelease(String.join(".", preReleaseParts)).getVersion();
  }

  private List<ModuleDefinition> updateModules(List<ModuleDefinition> modules, ModulesLoadResult modulesLoadResult) {
    return modules.stream()
      .map(old -> modulesLoadResult.artifacts().stream()
        .filter(md -> md.getName().equals(old.getName())).findFirst().orElse(old))
      .toList();
  }

  private List<Map<String, Object>> updateDescriptors(ModulesLoadResult modulesLoadResult,
                                                      List<Map<String, Object>> descriptors) {
    if (descriptors == null) {
      return null;
    }

    Map<String, Map<String, Object>> loadResults = modulesLoadResult.descriptors().stream()
      .map(descriptor -> parseModuleId(getModuleId(descriptor))
        .map(moduleId -> Pair.of(moduleId.getName(), descriptor)))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toMap(Pair::getKey, Pair::getValue, (d1, d2) -> d1));

    return descriptors.stream()
      .map(old -> parseModuleId(getModuleId(old))
        .map(moduleId -> ofNullable(loadResults.get(moduleId.getName())).orElse(old)).orElse(old))
      .toList();
  }

  private String getModuleId(Map<String, Object> descriptor) {
    return descriptor.get("id").toString();
  }

  private void validate(List<Dependency> modules, List<Dependency> updateModuleNameVersion) {
    var moduleNameVersion = modules.stream().collect(
      toMap(Dependency::getName, Dependency::getVersion, (v1, v2) -> v2));

    var invalid = updateModuleNameVersion.stream()
      .filter(dep -> isInvalidModule(dep.getName(), dep.getVersion(), moduleNameVersion))
      .toList();

    if (!invalid.isEmpty()) {
      throw new IllegalArgumentException("Invalid input modules to update:\n"
        + collectToBulletedList(invalid.stream().map(ApplicationDescriptorUpdateService::mapToErrorMessage).toList()));
    }
  }

  private boolean isInvalidModule(String name, String newVersion, Map<String, String> moduleNameVersion) {
    if (LATEST_VERSION.equals(newVersion)) {
      return false;
    }
    var oldVersion = moduleNameVersion.get(name);
    if (oldVersion == null) {
      return true;
    }
    var old = SemverUtils.parse(oldVersion);
    var newSemver = SemverUtils.parse(newVersion);
    return old == null || newSemver == null || old.isGreaterThanOrEqualTo(newSemver);
  }

  private List<ModuleDefinition> convertToArtifacts(List<Dependency> dependencies) {
    return emptyIfNull(dependencies).stream().map(this::toArtifact).toList();
  }

  private ModuleDefinition toArtifact(Dependency dependency) {
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

  private static String mapToErrorMessage(Dependency dependency) {
    return String.format("%s version older or the same: %s", dependency.getName(), dependency.getVersion());
  }
}
