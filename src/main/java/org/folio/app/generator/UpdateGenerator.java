package org.folio.app.generator;

import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

import java.util.List;
import javax.inject.Inject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ErrorDetail;
import org.folio.app.generator.service.ApplicationDescriptorUpdateService;
import org.folio.app.generator.service.JsonProvider;
import org.folio.app.generator.service.ModuleRegistryProvider;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;

@Mojo(name = "updateFromJson", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = RUNTIME)
public class UpdateGenerator extends AbstractUpdateMojo {

  @Parameter(defaultValue = "${modules}")
  String cmdModulesString;

  @Parameter(defaultValue = "${uiModules}")
  String cmdUiModulesString;

  @Parameter(name = "allowDowngrade", property = "allowDowngrade", defaultValue = "false")
  boolean allowDowngrade;

  @Parameter(name = "allowAddModules", property = "allowAddModules", defaultValue = "false")
  boolean allowAddModules;

  @Parameter(name = "removeUnlistedModules", property = "removeUnlistedModules", defaultValue = "false")
  boolean removeUnlistedModules;

  @Inject
  public UpdateGenerator(ModuleRegistryProvider registryProvider, ApplicationContextBuilder contextBuilder) {
    super(registryProvider, contextBuilder);
  }

  @Override
  public void execute() throws MojoExecutionException {
    var ctx = buildApplicationContext();
    var appName = mavenProject.getArtifactId();

    writeExecutionStarted(ctx, "updateFromJson", appName);

    try {
      var jsonProvider = ctx.getBean(JsonProvider.class);
      var application = jsonProvider.readJsonFromFile(appDescriptorPath, ApplicationDescriptor.class, false);

      var descriptorUpdateService = ctx.getBean(ApplicationDescriptorUpdateService.class);
      var updateConfig = buildUpdateConfig(allowDowngrade, allowAddModules, removeUnlistedModules);
      descriptorUpdateService.update(application, cmdModulesString, cmdUiModulesString, updateConfig);

      writeExecutionSuccess(ctx, "updateFromJson", appName, application.getVersion());
    } catch (Exception e) {
      var category = classifyException(e);
      List<ErrorDetail> errors = e instanceof ApplicationGeneratorException age ? age.getErrors() : List.of();
      writeExecutionFailure(ctx, "updateFromJson", appName, category, e.getMessage(), errors);
      throw toMojoExecutionException(e);
    }
  }
}
