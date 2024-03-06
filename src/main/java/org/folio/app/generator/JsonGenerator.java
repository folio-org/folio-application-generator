package org.folio.app.generator;

import static org.apache.maven.plugins.annotations.LifecyclePhase.INITIALIZE;
import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.folio.app.generator.service.ApplicationDescriptorGenerator;
import org.folio.app.generator.service.JsonTemplateProvider;

@Mojo(name = "generateFromJson", defaultPhase = INITIALIZE, requiresDependencyResolution = RUNTIME)
public class JsonGenerator extends AbstractGeneratorMojo {

  @Parameter(name = "templatePath", defaultValue = "${basedir}/application-template.json")
  String templatePath;

  @Override
  public void execute() throws MojoExecutionException {
    var ctx = buildApplicationContext();
    ctx.registerBean("jsonTemplateProvider", JsonTemplateProvider.class);

    var jsonTemplateProvider = ctx.getBean(JsonTemplateProvider.class);
    var template = jsonTemplateProvider.readTemplate(templatePath);

    var applicationDescriptorService = ctx.getBean(ApplicationDescriptorGenerator.class);
    applicationDescriptorService.generate(template);
  }
}
