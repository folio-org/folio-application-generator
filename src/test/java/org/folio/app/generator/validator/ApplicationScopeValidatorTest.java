package org.folio.app.generator.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.ApplicationScope;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.support.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ApplicationScopeValidatorTest {

  private static final List<Dependency> MODULES = List.of(
    new Dependency("mod-foo", "1.0.0", null, null),
    new Dependency("mod-bar", "^2.0.0", null, null));
  private static final List<Dependency> UI_MODULES = List.of(
    new Dependency("folio_foo", "1.0.0", null, null));

  @Test
  void validate_negative_platformScopeWithModules() {
    var template = new ApplicationDescriptorTemplate().scope(ApplicationScope.PLATFORM).modules(MODULES);

    assertThatThrownBy(() -> ApplicationScopeValidator.validate(template))
      .isInstanceOf(ApplicationGeneratorException.class)
      .hasMessage("Application with scope 'platform' must not define modules or uiModules "
        + "(modules: 2, uiModules: 0)")
      .extracting(e -> ((ApplicationGeneratorException) e).getCategory())
      .isEqualTo(ErrorCategory.CONFIGURATION_ERROR);
  }

  @Test
  void validate_negative_platformScopeWithUiModules() {
    var template = new ApplicationDescriptorTemplate().scope(ApplicationScope.PLATFORM).uiModules(UI_MODULES);

    assertThatThrownBy(() -> ApplicationScopeValidator.validate(template))
      .isInstanceOf(ApplicationGeneratorException.class)
      .hasMessage("Application with scope 'platform' must not define modules or uiModules "
        + "(modules: 0, uiModules: 1)");
  }

  @Test
  void validate_negative_platformScopeWithModulesAndUiModules() {
    var template = new ApplicationDescriptorTemplate()
      .scope(ApplicationScope.PLATFORM)
      .modules(MODULES)
      .uiModules(UI_MODULES);

    assertThatThrownBy(() -> ApplicationScopeValidator.validate(template))
      .isInstanceOf(ApplicationGeneratorException.class)
      .hasMessageContaining("(modules: 2, uiModules: 1)");
  }

  @Test
  void validate_positive_platformScopeWithNullModules() {
    var template = new ApplicationDescriptorTemplate().scope(ApplicationScope.PLATFORM);

    assertThatCode(() -> ApplicationScopeValidator.validate(template)).doesNotThrowAnyException();
    assertThat(template.getModules()).isNull();
    assertThat(template.getUiModules()).isNull();
  }

  @Test
  void validate_positive_platformScopeWithEmptyModules() {
    var template = new ApplicationDescriptorTemplate()
      .scope(ApplicationScope.PLATFORM)
      .modules(List.of())
      .uiModules(List.of());

    assertThatCode(() -> ApplicationScopeValidator.validate(template)).doesNotThrowAnyException();
  }

  @Test
  void validate_positive_platformScopeWithDependenciesOnly() {
    var template = new ApplicationDescriptorTemplate()
      .scope(ApplicationScope.PLATFORM)
      .dependencies(List.of(new Dependency("app-kafka", ">=3.5.1", null, null)));

    assertThatCode(() -> ApplicationScopeValidator.validate(template)).doesNotThrowAnyException();
  }

  @Test
  void validate_positive_regularScopeWithModules() {
    var template = new ApplicationDescriptorTemplate()
      .scope(ApplicationScope.REGULAR)
      .modules(MODULES)
      .uiModules(UI_MODULES);

    assertThatCode(() -> ApplicationScopeValidator.validate(template)).doesNotThrowAnyException();
  }

  @Test
  void validate_positive_missingScopeWithModules() {
    var template = new ApplicationDescriptorTemplate().modules(MODULES).uiModules(UI_MODULES);

    assertThatCode(() -> ApplicationScopeValidator.validate(template)).doesNotThrowAnyException();
  }
}
