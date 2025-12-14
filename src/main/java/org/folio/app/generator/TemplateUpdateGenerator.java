package org.folio.app.generator;

import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;
import static org.folio.app.generator.utils.PluginUtils.emptyIfNull;

import javax.inject.Inject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.service.ApplicationDescriptorUpdateService;
import org.folio.app.generator.service.JsonProvider;
import org.folio.app.generator.service.ModuleRegistryProvider;

@Mojo(name = "updateFromTemplate", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = RUNTIME)
public class TemplateUpdateGenerator extends AbstractUpdateMojo {

  @Parameter(name = "templatePath", property = "templatePath",
    defaultValue = "${basedir}/${project.artifactId}.template.json")
  String templatePath;

  @Parameter(name = "allowDowngrade", property = "allowDowngrade", defaultValue = "true")
  boolean allowDowngrade;

  @Parameter(name = "allowAddModules", property = "allowAddModules", defaultValue = "true")
  boolean allowAddModules;

  @Parameter(name = "removeUnlistedModules", property = "removeUnlistedModules", defaultValue = "true")
  boolean removeUnlistedModules;

  @Inject
  public TemplateUpdateGenerator(ModuleRegistryProvider registryProvider, ApplicationContextBuilder contextBuilder) {
    super(registryProvider, contextBuilder);
  }

  @Override
  public void execute() throws MojoExecutionException {
    var ctx = buildApplicationContext();

    var jsonProvider = ctx.getBean(JsonProvider.class);
    var application = jsonProvider.readJsonFromFile(appDescriptorPath, ApplicationDescriptor.class, false);
    var template = jsonProvider.readJsonFromFile(templatePath, ApplicationDescriptorTemplate.class, true);

    application.setDependencies(emptyIfNull(template.getDependencies()));

    var updateConfig = buildUpdateConfig(allowDowngrade, allowAddModules, removeUnlistedModules);

    var updateService = ctx.getBean(ApplicationDescriptorUpdateService.class);
    updateService.update(application,
      emptyIfNull(template.getModules()),
      emptyIfNull(template.getUiModules()),
      updateConfig);
  }
}
