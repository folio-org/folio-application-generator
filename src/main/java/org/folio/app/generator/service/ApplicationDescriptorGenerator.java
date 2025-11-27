package org.folio.app.generator.service;

import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.utils.PluginConfig;
import org.folio.app.generator.validator.ApplicationDependencyValidator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationDescriptorGenerator {

  private final MavenProject mavenProject;
  private final PluginConfig pluginConfig;
  private final JsonProvider jsonProvider;
  private final ApplicationDescriptorService applicationDescriptorService;
  private final ApplicationDependencyValidator applicationDependencyValidator;

  /**
   * Generates application descriptor from template.
   *
   * @param template - application descriptor {@link ApplicationDescriptorTemplate} object
   * @throws MojoExecutionException if application description was failed to generate
   */
  public void generate(ApplicationDescriptorTemplate template) throws MojoExecutionException {
    applicationDependencyValidator.validateDependencies(template);
    var resolved = applicationDescriptorService.create(template);

    var outputDir = mavenProject.getBuild().getDirectory();
    var mode = pluginConfig.getModuleUrlsMode();

    if (mode.needFullDescriptor()) {
      jsonProvider.writeApplication(resolved.toFullDescriptor(), outputDir);
    }

    if (mode.needDescriptorUrl()) {
      var filename = mode.needBoth()
        ? resolved.getId() + ".url.json"
        : resolved.getId() + ".json";
      jsonProvider.writeApplication(resolved.toUrlOnlyDescriptor(), outputDir, filename);
    }
  }
}
