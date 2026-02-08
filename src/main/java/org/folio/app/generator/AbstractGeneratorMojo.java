package org.folio.app.generator;

import static java.lang.Boolean.parseBoolean;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import javax.inject.Inject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ErrorDetail;
import org.folio.app.generator.model.ExecutionResult;
import org.folio.app.generator.model.registry.ConfigModuleRegistry;
import org.folio.app.generator.model.registry.artifact.ConfigArtifactRegistry;
import org.folio.app.generator.model.types.ErrorCategory;
import org.folio.app.generator.service.JsonProvider;
import org.folio.app.generator.service.ModuleRegistryProvider;
import org.folio.app.generator.service.exceptions.ApplicationGeneratorException;
import org.folio.app.generator.utils.PluginConfig;
import org.springframework.context.support.GenericApplicationContext;
import software.amazon.awssdk.regions.Region;

public abstract class AbstractGeneratorMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  protected MavenProject mavenProject;

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  protected MavenSession mavenSession;

  @Parameter(defaultValue = "${buildNumber}")
  protected String buildNumber;

  @Parameter(defaultValue = "${registries}")
  protected String cmdRegistriesString;

  @Parameter(defaultValue = "${beRegistries}")
  protected String cmdBeRegistriesString;

  @Parameter(defaultValue = "${uiRegistries}")
  protected String cmdUiRegistriesString;

  @Parameter(defaultValue = "${overrideConfigRegistries}")
  protected String overrideConfigRegistries;

  @Parameter(name = "moduleRegistries")
  protected List<ConfigModuleRegistry> moduleRegistries;

  @Parameter(name = "beModuleRegistries")
  protected List<ConfigModuleRegistry> beModuleRegistries;

  @Parameter(name = "uiModuleRegistries")
  protected List<ConfigModuleRegistry> uiModuleRegistries;

  @Parameter(defaultValue = "${fallbackRegistries}")
  protected String cmdFallbackRegistriesString;

  @Parameter(defaultValue = "${beFallbackRegistries}")
  protected String cmdBeFallbackRegistriesString;

  @Parameter(defaultValue = "${uiFallbackRegistries}")
  protected String cmdUiFallbackRegistriesString;

  @Parameter(name = "fallbackModuleRegistries")
  protected List<ConfigModuleRegistry> fallbackModuleRegistries;

  @Parameter(name = "beFallbackModuleRegistries")
  protected List<ConfigModuleRegistry> beFallbackModuleRegistries;

  @Parameter(name = "uiFallbackModuleRegistries")
  protected List<ConfigModuleRegistry> uiFallbackModuleRegistries;

  @Parameter(name = "moduleUrlsOnly", property = "moduleUrlsOnly", defaultValue = "false")
  protected String moduleUrlsOnly;

  @Parameter(defaultValue = "${awsRegion}")
  protected String awsRegion;

  @Parameter(name = "validateArtifacts", property = "validateArtifacts", defaultValue = "false")
  protected String validateArtifacts;

  @Parameter(name = "artifactRegistries")
  protected List<ConfigArtifactRegistry> artifactRegistries;

  @Parameter(name = "beArtifactRegistries")
  protected List<ConfigArtifactRegistry> beArtifactRegistries;

  @Parameter(name = "uiArtifactRegistries")
  protected List<ConfigArtifactRegistry> uiArtifactRegistries;

  @Parameter(name = "bePreReleaseArtifactRegistries")
  protected List<ConfigArtifactRegistry> bePreReleaseArtifactRegistries;

  @Parameter(name = "uiPreReleaseArtifactRegistries")
  protected List<ConfigArtifactRegistry> uiPreReleaseArtifactRegistries;

  @Parameter(defaultValue = "${artifactRegistries}")
  protected String cmdArtifactRegistries;

  @Parameter(defaultValue = "${beArtifactRegistries}")
  protected String cmdBeArtifactRegistries;

  @Parameter(defaultValue = "${uiArtifactRegistries}")
  protected String cmdUiArtifactRegistries;

  @Parameter(defaultValue = "${bePreReleaseArtifactRegistries}")
  protected String cmdBePreReleaseArtifactRegistries;

  @Parameter(defaultValue = "${uiPreReleaseArtifactRegistries}")
  protected String cmdUiPreReleaseArtifactRegistries;

  protected final ModuleRegistryProvider moduleRegistryProvider;
  protected final ApplicationContextBuilder applicationContextBuilder;

  @Inject
  protected AbstractGeneratorMojo(ModuleRegistryProvider registryProvider, ApplicationContextBuilder contextBuilder) {
    this.moduleRegistryProvider = registryProvider;
    this.applicationContextBuilder = contextBuilder;
  }

  protected GenericApplicationContext buildApplicationContext() {
    var pluginConfig = PluginConfig.builder()
      .buildNumber(buildNumber)
      .registries(moduleRegistries)
      .uiRegistries(uiModuleRegistries)
      .beRegistries(beModuleRegistries)
      .cmdRegistryString(cmdRegistriesString)
      .beCmdRegistryString(cmdBeRegistriesString)
      .uiCmdRegistryString(cmdUiRegistriesString)
      .fallbackRegistries(fallbackModuleRegistries)
      .beFallbackRegistries(beFallbackModuleRegistries)
      .uiFallbackRegistries(uiFallbackModuleRegistries)
      .cmdFallbackRegistryString(cmdFallbackRegistriesString)
      .beCmdFallbackRegistryString(cmdBeFallbackRegistriesString)
      .uiCmdFallbackRegistryString(cmdUiFallbackRegistriesString)
      .overrideConfigRegistries(parseBoolean(overrideConfigRegistries))
      .moduleUrlsOnly(parseBoolean(moduleUrlsOnly))
      .awsRegion(isNotBlank(awsRegion) ? Region.of(awsRegion) : Region.US_EAST_1)
      .validateArtifacts(parseBoolean(validateArtifacts))
      .artifactRegistries(artifactRegistries)
      .beArtifactRegistries(beArtifactRegistries)
      .uiArtifactRegistries(uiArtifactRegistries)
      .bePreReleaseArtifactRegistries(bePreReleaseArtifactRegistries)
      .uiPreReleaseArtifactRegistries(uiPreReleaseArtifactRegistries)
      .cmdArtifactRegistries(cmdArtifactRegistries)
      .cmdBeArtifactRegistries(cmdBeArtifactRegistries)
      .cmdUiArtifactRegistries(cmdUiArtifactRegistries)
      .cmdBePreReleaseArtifactRegistries(cmdBePreReleaseArtifactRegistries)
      .cmdUiPreReleaseArtifactRegistries(cmdUiPreReleaseArtifactRegistries)
      .build();

    var registries = moduleRegistryProvider.getModuleRegistries(pluginConfig);

    return applicationContextBuilder
      .withLog(getLog())
      .withMavenProject(mavenProject)
      .withMavenSession(mavenSession)
      .withPluginConfig(pluginConfig)
      .withModuleRegistries(registries)
      .build();
  }

  protected void writeExecutionStarted(GenericApplicationContext ctx, String goal, String appName)
      throws ApplicationGeneratorException {
    var jsonProvider = ctx.getBean(JsonProvider.class);
    var result = ExecutionResult.started(goal, appName);
    jsonProvider.writeExecutionResult(result, mavenProject.getBuild().getDirectory());
  }

  protected void writeExecutionSuccess(GenericApplicationContext ctx, String goal, String appName, String appVersion,
                                       boolean changesDetected) throws ApplicationGeneratorException {
    var jsonProvider = ctx.getBean(JsonProvider.class);
    var result = ExecutionResult.success(goal, appName, appVersion, changesDetected);
    jsonProvider.writeExecutionResult(result, mavenProject.getBuild().getDirectory());
  }

  protected void writeExecutionFailure(GenericApplicationContext ctx, String goal, String appName,
                                       ErrorCategory category, String message, List<ErrorDetail> errors)
      throws ApplicationGeneratorException {
    var jsonProvider = ctx.getBean(JsonProvider.class);
    var result = ExecutionResult.failure(goal, appName, category, message, errors);
    jsonProvider.writeExecutionResult(result, mavenProject.getBuild().getDirectory());
  }

  protected ErrorCategory classifyException(Exception e) {
    return ErrorCategory.fromException(e);
  }

  protected static MojoExecutionException toMojoExecutionException(Exception e) {
    return e instanceof MojoExecutionException mee ? mee
      : new MojoExecutionException(e.getMessage(), e);
  }
}
