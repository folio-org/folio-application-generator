package org.folio.app.generator;

import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Parameter;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.UpdateConfig;
import org.folio.app.generator.service.ModuleRegistryProvider;

public abstract class AbstractUpdateMojo extends AbstractGeneratorMojo {

  @Parameter(name = "appDescriptorPath", property = "appDescriptorPath",
    defaultValue = "${basedir}/${project.artifactId}-${project.version}.json")
  protected String appDescriptorPath;

  @Parameter(name = "useProjectVersion", property = "useProjectVersion", defaultValue = "false")
  protected boolean useProjectVersion;

  @Parameter(name = "noVersionBump", property = "noVersionBump", defaultValue = "false")
  protected boolean noVersionBump;

  @Inject
  protected AbstractUpdateMojo(ModuleRegistryProvider registryProvider, ApplicationContextBuilder contextBuilder) {
    super(registryProvider, contextBuilder);
  }

  protected UpdateConfig buildUpdateConfig(boolean allowDowngrade, boolean allowAddModules,
                                           boolean removeUnlistedModules) {
    return UpdateConfig.builder()
      .allowDowngrade(allowDowngrade)
      .allowAddModules(allowAddModules)
      .removeUnlistedModules(removeUnlistedModules)
      .useProjectVersion(useProjectVersion)
      .noVersionBump(noVersionBump)
      .build();
  }
}
