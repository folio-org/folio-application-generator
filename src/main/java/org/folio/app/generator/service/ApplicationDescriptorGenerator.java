package org.folio.app.generator.service;

import java.io.File;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.utils.JsonConverter;
import org.folio.app.generator.validator.ApplicationDependencyValidator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationDescriptorGenerator {

  private final MavenProject mavenProject;
  private final JsonConverter jsonConverter;
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

    var targetDirectory = new File(mavenProject.getBuild().getDirectory());
    if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
      throw new MojoExecutionException("Could not create target directory: " + targetDirectory);
    }

    var applicationDescriptorFile = new File(targetDirectory, application.getId() + ".json");
    jsonConverter.writeValue(applicationDescriptorFile, application);
  }
}
