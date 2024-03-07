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
import org.folio.app.generator.model.registry.ConfigModuleRegistry;
import org.folio.app.generator.service.ModuleRegistryProvider;
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

  @Parameter(defaultValue = "${overrideConfigRegistries}")
  protected String overrideConfigRegistries;

  @Parameter(name = "moduleRegistries")
  protected List<ConfigModuleRegistry> moduleRegistries;

  @Parameter(name = "useModuleDescriptorsUrls")
  protected String useModuleDescriptorsUrls;

  @Parameter(defaultValue = "${awsRegion}")
  protected String awsRegion;

  protected final ModuleRegistryProvider moduleRegistryProvider;
  protected final ApplicationContextBuilder applicationContextBuilder;

  @Inject
  public AbstractGeneratorMojo(ModuleRegistryProvider registryProvider, ApplicationContextBuilder contextBuilder) {
    this.moduleRegistryProvider = registryProvider;
    this.applicationContextBuilder = contextBuilder;
  }

  protected GenericApplicationContext buildApplicationContext() throws MojoExecutionException {
    var pluginConfig = PluginConfig.builder()
      .buildNumber(buildNumber)
      .registries(moduleRegistries)
      .cmdRegistryString(cmdRegistriesString)
      .overrideConfigRegistries(parseBoolean(overrideConfigRegistries))
      .useModuleDescriptorsUrls(parseBoolean(useModuleDescriptorsUrls))
      .awsRegion(isNotBlank(awsRegion) ? Region.of(awsRegion) : Region.US_EAST_1)
      .build();

    var registries = moduleRegistryProvider.validateAndGetModuleRegistries(pluginConfig);

    return applicationContextBuilder
      .withLog(getLog())
      .withMavenProject(mavenProject)
      .withMavenSession(mavenSession)
      .withPluginConfig(pluginConfig)
      .withModuleRegistries(registries)
      .build();
  }
}
