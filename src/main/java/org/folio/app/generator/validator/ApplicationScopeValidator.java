package org.folio.app.generator.validator;

import static java.lang.String.format;
import static org.folio.app.generator.model.types.ErrorCategory.CONFIGURATION_ERROR;
import static org.folio.app.generator.utils.PluginUtils.emptyIfNull;

import lombok.experimental.UtilityClass;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.ApplicationScope;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;

@UtilityClass
public class ApplicationScopeValidator {

  /**
   * Ensures that a template with {@link ApplicationScope#PLATFORM} scope does not define modules or uiModules.
   *
   * @param template - {@link ApplicationDescriptorTemplate} to analyze
   * @throws ApplicationGeneratorException if platform template has non-empty modules or uiModules
   */
  public static void validate(ApplicationDescriptorTemplate template) {
    if (template.getScope() != ApplicationScope.PLATFORM) {
      return;
    }

    int modulesCount = emptyIfNull(template.getModules()).size();
    int uiModulesCount = emptyIfNull(template.getUiModules()).size();
    if (modulesCount > 0 || uiModulesCount > 0) {
      throw new ApplicationGeneratorException(
        format("Application with scope 'platform' must not define modules or uiModules (modules: %d, uiModules: %d)",
          modulesCount, uiModulesCount),
        CONFIGURATION_ERROR);
    }
  }
}
