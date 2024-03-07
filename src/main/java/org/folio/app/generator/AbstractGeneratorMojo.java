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
  MavenProject mavenProject;

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  MavenSession mavenSession;

  @Parameter(defaultValue = "${buildNumber}")
  String buildNumber;

  @Parameter(defaultValue = "${registries}")
  String cmdRegistriesString;

  @Parameter(defaultValue = "${overrideConfigRegistries}")
  String overrideConfigRegistries;

  @Parameter(name = "moduleRegistries")
  List<ConfigModuleRegistry> moduleRegistries;

  @Parameter(name = "useModuleDescriptorsUrls")
  String useModuleDescriptorsUrls;

  @Parameter(defaultValue = "${awsRegion}")
  String awsRegion;

  @Inject ModuleRegistryProvider moduleRegistryProvider;
  @Inject ApplicationContextBuilder applicationContextBuilder;

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
