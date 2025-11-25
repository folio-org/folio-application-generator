package org.folio.app.generator.validator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.PreReleaseFilter;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ApplicationDependencyValidatorTest {

  @Mock private Log log;
  @Mock private MavenProject mavenProject;

  private ApplicationDependencyValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ApplicationDependencyValidator(log, mavenProject);
  }

  @Test
  void validateDependencies_positive_validModules() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("mod-foo", "1.0.0", PreReleaseFilter.FALSE),
      new Dependency("mod-bar", "^2.0.0", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    validator.validateDependencies(template);
  }

  @Test
  void validateDependencies_positive_validApplicationDependencies() {
    var template = new ApplicationDescriptorTemplate();
    template.setDependencies(List.of(
      new Dependency("app-foo", "1.0.0", PreReleaseFilter.FALSE),
      new Dependency("app-bar", "^2.0.0", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    validator.validateDependencies(template);
  }

  @Test
  void validateDependencies_positive_preReleaseVersionWithTrueFilter() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("mod-foo", "1.0.0-SNAPSHOT", PreReleaseFilter.TRUE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    validator.validateDependencies(template);
  }

  @Test
  void validateDependencies_positive_preReleaseVersionWithOnlyFilter() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("mod-foo", "1.0.0-alpha", PreReleaseFilter.ONLY)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    validator.validateDependencies(template);
  }

  @Test
  void validateDependencies_positive_stableVersionWithFalseFilter() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("mod-foo", "1.0.0", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    validator.validateDependencies(template);
  }

  @Test
  void validateDependencies_positive_stableVersionWithNullFilter() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("mod-foo", "1.0.0", null)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    validator.validateDependencies(template);
  }

  @Test
  void validateDependencies_positive_reservedVersionKeyword() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("mod-foo", "", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    validator.validateDependencies(template);
  }

  @Test
  void validateDependencies_positive_versionConstraint() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("mod-foo", "^1.0.0", PreReleaseFilter.FALSE),
      new Dependency("mod-bar", "~2.0.0", PreReleaseFilter.FALSE),
      new Dependency("mod-baz", ">=3.0.0", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    validator.validateDependencies(template);
  }

  @Test
  void validateDependencies_positive_templateVersionOverridesProjectVersion() {
    var template = new ApplicationDescriptorTemplate();
    template.setVersion("2.0.0");
    template.setModules(List.of(
      new Dependency("mod-foo", "1.0.0", PreReleaseFilter.FALSE)));

    validator.validateDependencies(template);
  }

  @Test
  void validateDependencies_positive_emptyModulesAndDependencies() {
    var template = new ApplicationDescriptorTemplate();

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    validator.validateDependencies(template);
  }

  @Test
  void validateDependencies_negative_emptyModuleName() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("", "1.0.0", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    assertThatThrownBy(() -> validator.validateDependencies(template))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid dependencies found")
        .hasMessageContaining("Module name cannot be empty at index: 0");
  }

  @Test
  void validateDependencies_negative_nullModuleName() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency(null, "1.0.0", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    assertThatThrownBy(() -> validator.validateDependencies(template))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid dependencies found")
        .hasMessageContaining("Module name cannot be empty at index: 0");
  }

  @Test
  void validateDependencies_negative_invalidModuleVersion() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("mod-foo", "invalid", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    assertThatThrownBy(() -> validator.validateDependencies(template))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid dependencies found")
        .hasMessageContaining("Module 'mod-foo' version 'invalid' must be a valid semver version or constraint");
  }

  @Test
  void validateDependencies_negative_preReleaseVersionWithFalseFilter() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("mod-foo", "1.0.0-SNAPSHOT", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    assertThatThrownBy(() -> validator.validateDependencies(template))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid dependencies found")
        .hasMessageContaining("Module 'mod-foo' version '1.0.0-SNAPSHOT' must be pre release");
  }

  @Test
  void validateDependencies_negative_stableVersionWithTrueFilter() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("mod-foo", "1.0.0", PreReleaseFilter.TRUE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    assertThatThrownBy(() -> validator.validateDependencies(template))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid dependencies found")
        .hasMessageContaining("Module 'mod-foo' version '1.0.0' must be stable");
  }

  @Test
  void validateDependencies_negative_stableVersionWithOnlyFilter() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("mod-foo", "1.0.0", PreReleaseFilter.ONLY)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    assertThatThrownBy(() -> validator.validateDependencies(template))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid dependencies found")
        .hasMessageContaining("Module 'mod-foo' version '1.0.0' must be stable");
  }

  @Test
  void validateDependencies_negative_emptyApplicationDependencyName() {
    var template = new ApplicationDescriptorTemplate();
    template.setDependencies(List.of(
      new Dependency("", "^1.0.0", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    assertThatThrownBy(() -> validator.validateDependencies(template))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid dependencies found")
        .hasMessageContaining("Module name cannot be empty at index: 0");
  }

  @Test
  void validateDependencies_negative_invalidApplicationDependencyVersion() {
    var template = new ApplicationDescriptorTemplate();
    template.setDependencies(List.of(
      new Dependency("app-foo", "invalid", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    assertThatThrownBy(() -> validator.validateDependencies(template))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid dependencies found")
        .hasMessageContaining("Application dependency 'app-foo' version 'invalid' must satisfy semver range");
  }

  @Test
  void validateDependencies_negative_multipleValidationErrors() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("", "1.0.0", PreReleaseFilter.FALSE),
      new Dependency("mod-bar", "invalid", PreReleaseFilter.FALSE),
      new Dependency("mod-baz", "1.0.0-SNAPSHOT", PreReleaseFilter.FALSE)));
    template.setDependencies(List.of(
      new Dependency("", "^1.0.0", PreReleaseFilter.FALSE),
      new Dependency("app-foo", "bad-version", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("1.0.0");

    assertThatThrownBy(() -> validator.validateDependencies(template))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid dependencies found")
        .hasMessageContaining("Module name cannot be empty at index: 0")
        .hasMessageContaining("Module 'mod-bar' version 'invalid' must be a valid semver version or constraint")
        .hasMessageContaining("Module 'mod-baz' version '1.0.0-SNAPSHOT' must be pre release")
        .hasMessageContaining("Application dependency 'app-foo' version 'bad-version' must satisfy semver range");
  }

  @Test
  void validateDependencies_negative_invalidTemplateVersion() {
    var template = new ApplicationDescriptorTemplate();
    template.setVersion("invalid");
    template.setModules(List.of(
      new Dependency("mod-foo", "1.0.0", PreReleaseFilter.FALSE)));

    assertThatThrownBy(() -> validator.validateDependencies(template))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Template version must satisfy semver: invalid");
  }

  @Test
  void validateDependencies_negative_invalidProjectVersion() {
    var template = new ApplicationDescriptorTemplate();
    template.setModules(List.of(
      new Dependency("mod-foo", "1.0.0", PreReleaseFilter.FALSE)));

    when(mavenProject.getVersion()).thenReturn("invalid-version");

    assertThatThrownBy(() -> validator.validateDependencies(template))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Project version must satisfy semver: invalid-version");
  }
}
