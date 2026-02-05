package org.folio.app.generator;

import java.util.List;
import javax.inject.Inject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.ErrorDetail;
import org.folio.app.generator.service.ApplicationDescriptorGenerator;
import org.folio.app.generator.service.ModuleRegistryProvider;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;

@Mojo(name = "generateFromConfiguration", defaultPhase = LifecyclePhase.COMPILE)
public class ConfigurationGenerator extends AbstractGeneratorMojo {

  @Parameter(name = "modules")
  List<Dependency> modules;

  @Parameter(name = "uiModules")
  List<Dependency> uiModules;

  @Parameter(name = "dependencies")
  List<Dependency> dependencies;

  @Inject
  public ConfigurationGenerator(ModuleRegistryProvider registryProvider, ApplicationContextBuilder contextBuilder) {
    super(registryProvider, contextBuilder);
  }

  @Override
  public void execute() throws MojoExecutionException {
    var ctx = buildApplicationContext();
    var appName = mavenProject.getArtifactId();

    writeExecutionStarted(ctx, "generateFromConfiguration", appName);

    try {
      var template = new ApplicationDescriptorTemplate()
        .dependencies(dependencies)
        .modules(modules)
        .uiModules(uiModules);

      var appDescriptorGenerator = ctx.getBean(ApplicationDescriptorGenerator.class);
      var application = appDescriptorGenerator.generate(template);

      writeExecutionSuccess(ctx, "generateFromConfiguration", appName, application.getVersion(), true);
    } catch (Exception e) {
      var category = classifyException(e);
      List<ErrorDetail> errors = e instanceof ApplicationGeneratorException age ? age.getErrors() : List.of();
      writeExecutionFailure(ctx, "generateFromConfiguration", appName, category, e.getMessage(), errors);
      throw toMojoExecutionException(e);
    }
  }
}
