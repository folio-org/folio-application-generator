package org.folio.app.generator.service;

import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.validator.ApplicationDependencyValidator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationDescriptorGenerator {

  private final MavenProject mavenProject;
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
    var application = applicationDescriptorService.create(template);

    jsonProvider.writeApplication(application, mavenProject.getBuild().getDirectory());
  }
}
