package org.folio.app.generator.validator;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.Dependency;
import org.semver4j.Semver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationDependencyValidator {

  private final Log log;
  private final MavenProject mavenProject;
  private final Set<String> reservedVersionKeywords = Set.of("latest");

  /**
   * Validates that all dependencies satisfies specific release and declared in a right way.
   *
   * @param template - {@link ApplicationDescriptorTemplate} to analyze
   * @throws MojoExecutionException - if any validation error occurred
   */
  public void validateDependencies(ApplicationDescriptorTemplate template) throws MojoExecutionException {
    var projectVersion = validateAndGetProjectVersion(template);
    var errors = Stream.of(template.getModules(), template.getUiModules(), template.getDependencies())
      .map(dependencies -> getValidationErrors(projectVersion, dependencies))
      .flatMap(Collection::stream)
      .toList();

    if (!errors.isEmpty()) {
      var errorsString = errors.stream().collect(Collectors.joining("\n  * ", "  * ", ""));
      throw new IllegalArgumentException("Invalid dependencies found:\n" + errorsString);
    }
  }

  private List<String> getValidationErrors(Semver projectVersion, List<Dependency> dependencies) {
    if (dependencies == null || dependencies.isEmpty()) {
      return emptyList();
    }

    var errors = new ArrayList<String>();
    boolean isProjectHasFixVersion = projectVersion.getPreRelease().isEmpty();
    log.debug("Is pre-release application descriptor: " + isProjectHasFixVersion);

    for (int i = 0; i < dependencies.size(); i++) {
      var dependency = dependencies.get(i);
      var validationError = validateDependency(dependency, i, isProjectHasFixVersion);
      if (validationError != null) {
        errors.add(validationError);
      }
    }

    return errors;
  }

  private String validateDependency(Dependency dependency, int i, boolean isProjectHasFixVersion) {
    var name = dependency.getName();
    if (StringUtils.isBlank(name)) {
      return "Dependency name cannot be empty at index: " + i;
    }

    var version = dependency.getVersion();
    var parsedVersion = Semver.parse(version);

    if (parsedVersion == null) {
      if (!reservedVersionKeywords.contains(version)) {
        return format("Dependency '%s' version '%s' must satisfy semver", name, version);
      }

      if (isProjectHasFixVersion) {
        return format("Dependency '%s' version '%s' must be stable for a stable release", name, version);
      }
    }

    if (isProjectHasFixVersion && !parsedVersion.getPreRelease().isEmpty()) {
      return format("Dependency '%s' version '%s' must be stable for a stable release", name, version);
    }

    return null;
  }

  private Semver validateAndGetProjectVersion(ApplicationDescriptorTemplate template) throws MojoExecutionException {
    var templateVersion = template.getVersion();
    if (templateVersion != null) {
      var parsedTemplateVersion = Semver.parse(templateVersion);
      if (parsedTemplateVersion == null) {
        throw new MojoExecutionException("Template version must satisfy semver: " + templateVersion);
      }

      return parsedTemplateVersion;
    }

    var projectVersion = mavenProject.getVersion();
    var parsedProjectVersion = Semver.parse(projectVersion);
    if (parsedProjectVersion == null) {
      throw new MojoExecutionException("Project version must satisfy semver: " + projectVersion);
    }

    return parsedProjectVersion;
  }
}
