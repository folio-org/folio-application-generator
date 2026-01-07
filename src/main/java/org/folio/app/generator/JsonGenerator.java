package org.folio.app.generator;

import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

import java.util.List;
import javax.inject.Inject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.ErrorDetail;
import org.folio.app.generator.service.ApplicationDescriptorGenerator;
import org.folio.app.generator.service.JsonProvider;
import org.folio.app.generator.service.ModuleRegistryProvider;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;

@Mojo(name = "generateFromJson", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = RUNTIME)
public class JsonGenerator extends AbstractGeneratorMojo {

  @Parameter(name = "templatePath", property = "templatePath",
    defaultValue = "${basedir}/${project.artifactId}.template.json")
  String templatePath;

  @Inject
  public JsonGenerator(ModuleRegistryProvider registryProvider, ApplicationContextBuilder contextBuilder) {
    super(registryProvider, contextBuilder);
  }

  @Override
  public void execute() throws MojoExecutionException {
    var ctx = buildApplicationContext();
    var appName = mavenProject.getArtifactId();

    writeExecutionStarted(ctx, "generateFromJson", appName);

    try {
      var applicationDescriptorService = ctx.getBean(ApplicationDescriptorGenerator.class);
      applicationDescriptorService.generate(readTemplate());
      writeExecutionSuccess(ctx, "generateFromJson", appName, mavenProject.getVersion());
    } catch (Exception e) {
      var category = classifyException(e);
      List<ErrorDetail> errors = e instanceof ApplicationGeneratorException age ? age.getErrors() : List.of();
      writeExecutionFailure(ctx, "generateFromJson", appName, category, e.getMessage(), errors);
      throw toMojoExecutionException(e);
    }
  }

  protected ApplicationDescriptorTemplate readTemplate() throws MojoExecutionException {
    var ctx = buildApplicationContext();
    var jsonProvider = ctx.getBean(JsonProvider.class);
    return jsonProvider.readJsonFromFile(templatePath, ApplicationDescriptorTemplate.class, true);
  }
}
