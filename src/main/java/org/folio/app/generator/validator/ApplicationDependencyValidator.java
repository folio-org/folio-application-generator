package org.folio.app.generator.validator;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.folio.app.generator.utils.PluginUtils.collectToBulletedList;
import static org.folio.app.generator.utils.PluginUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.PreReleaseFilter;
import org.semver4j.RangesListFactory;
import org.semver4j.Semver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationDependencyValidator {

  private final Log log;
  private final MavenProject mavenProject;
  private final Set<String> reservedVersionKeywords = Set.of("");

  /**
   * Validates that all dependencies satisfies specific release and declared in a right way.
   *
   * @param template - {@link ApplicationDescriptorTemplate} to analyze
   */
  public void validateDependencies(ApplicationDescriptorTemplate template) {
    var projectVersion = validateAndGetProjectVersion(template);
    var errors = Stream.of(emptyIfNull(template.getModules()))
      .map(moduleDependencies -> validateModules(projectVersion, moduleDependencies))
      .flatMap(Collection::stream)
      .collect(toList());

    errors.addAll(validateApplicationDependencies(emptyIfNull(template.getDependencies())));

    if (!errors.isEmpty()) {
      throw new IllegalArgumentException("Invalid dependencies found:\n" + collectToBulletedList(errors));
    }
  }

  private List<String> validateModules(Semver projectVersion, List<Dependency> dependencies) {
    var errors = new ArrayList<String>();

    for (int i = 0; i < dependencies.size(); i++) {
      var dependency = dependencies.get(i);
      var validationError = validateModuleDefinition(dependency, i);
      if (validationError != null) {
        errors.add(validationError);
      }
    }

    return errors;
  }

  private String validateModuleDefinition(Dependency dependency, int idx) {
    var name = dependency.getName();
    if (StringUtils.isBlank(name)) {
      return "Module name cannot be empty at index: " + idx;
    }

    var version = dependency.getVersion();

    if (reservedVersionKeywords.contains(version)) {
      return null;
    }

    var rangesList = RangesListFactory.create(version).get();
    if (rangesList.isEmpty()) {
      return format("Module '%s' version '%s' must be a valid semver version or constraint", name, version);
    }

    var parsed = Semver.parse(version);
    if (parsed == null) {
      return null;
    }

    var preRelease = dependency.getPreRelease() == null ? PreReleaseFilter.FALSE : dependency.getPreRelease();
    boolean isPre = !parsed.getPreRelease().isEmpty();

    if (!isPre && preRelease != PreReleaseFilter.FALSE) {
      return format("Module '%s' version '%s' must be stable", name, version);
    }

    if (isPre && preRelease == PreReleaseFilter.FALSE) {
      return format("Module '%s' version '%s' must be pre release", name, version);
    }

    return null;
  }

  private List<String> validateApplicationDependencies(List<Dependency> dependencies) {
    var errors = new ArrayList<String>();
    for (int i = 0; i < dependencies.size(); i++) {
      var dependency = dependencies.get(i);
      var validationError = validateApplicationDependency(i, dependency);
      if (validationError != null) {
        errors.add(validationError);
      }
    }

    return errors;
  }

  private String validateApplicationDependency(int idx, Dependency dependency) {
    var name = dependency.getName();
    if (StringUtils.isBlank(name)) {
      return "Module name cannot be empty at index: " + idx;
    }

    var version = dependency.getVersion();
    var rangesList = RangesListFactory.create(version).get();
    if (rangesList.isEmpty()) {
      return format("Application dependency '%s' version '%s' must satisfy semver range", name, version);
    }

    return null;
  }

  private Semver validateAndGetProjectVersion(ApplicationDescriptorTemplate template) {
    var templateVersion = template.getVersion();
    if (templateVersion != null) {
      var parsedTemplateVersion = Semver.parse(templateVersion);
      if (parsedTemplateVersion == null) {
        throw new IllegalArgumentException("Template version must satisfy semver: " + templateVersion);
      }

      return parsedTemplateVersion;
    }

    var projectVersion = mavenProject.getVersion();
    var parsedProjectVersion = Semver.parse(projectVersion);
    if (parsedProjectVersion == null) {
      throw new IllegalArgumentException("Project version must satisfy semver: " + projectVersion);
    }

    return parsedProjectVersion;
  }
}
