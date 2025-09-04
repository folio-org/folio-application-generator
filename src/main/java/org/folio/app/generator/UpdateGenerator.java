package org.folio.app.generator;

import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

import javax.inject.Inject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.service.ApplicationDescriptorUpdateService;
import org.folio.app.generator.service.JsonProvider;
import org.folio.app.generator.service.ModuleRegistryProvider;

@Mojo(name = "updateFromJson", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = RUNTIME)
public class UpdateGenerator extends AbstractGeneratorMojo {

  @Parameter(name = "appDescriptorPath", defaultValue = "${basedir}/application-descriptor.json")
  String appDescriptorPath;

  @Parameter(defaultValue = "${modules}")
  String cmdModulesString;

  @Parameter(defaultValue = "${uiModules}")
  String cmdUiModulesString;

  @Inject
  public UpdateGenerator(ModuleRegistryProvider registryProvider, ApplicationContextBuilder contextBuilder) {
    super(registryProvider, contextBuilder);
  }

  @Override
  public void execute() throws MojoExecutionException {
    var ctx = buildApplicationContext();

    var jsonProvider = ctx.getBean(JsonProvider.class);
    var application = jsonProvider.readJsonFromFile(appDescriptorPath, ApplicationDescriptor.class, false);

    var descriptorUpdateService = ctx.getBean(ApplicationDescriptorUpdateService.class);
    descriptorUpdateService.update(application, cmdModulesString, cmdUiModulesString);
  }
}
