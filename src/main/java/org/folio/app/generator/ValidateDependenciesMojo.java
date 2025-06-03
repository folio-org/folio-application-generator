package org.folio.app.generator;

import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.folio.app.generator.configuration.ApplicationContextBuilder;
import org.folio.app.generator.service.ApplicationDescriptorService;
import org.folio.app.generator.service.ModuleRegistryProvider;
import org.folio.app.generator.service.loader.ApplicationModulesValidator;

@Mojo(name = "validateDependencies")
public class ValidateDependenciesMojo extends JsonGenerator {

  @Parameter(property = "baseUrl", required = true)
  String baseUrl;

  @Parameter(property = "token", required = true)
  String token;

  @Inject
  public ValidateDependenciesMojo(ModuleRegistryProvider registryProvider,
                                  ApplicationContextBuilder contextBuilder) {
    super(registryProvider, contextBuilder);
  }

  @Override
  public void execute() throws MojoExecutionException {

    var ctx = buildApplicationContext();
    var applicationDescriptorService = ctx.getBean(ApplicationDescriptorService.class);
    var applicationModulesValidator = ctx.getBean(ApplicationModulesValidator.class);

    var template = readTemplate();
    var application = applicationDescriptorService.create(template);

    applicationModulesValidator.validateApplication(application, baseUrl, token);
  }

  private final void prevalidateParameters() throws MojoExecutionException {
    if (StringUtils.isBlank(baseUrl)) {
      throw new MojoExecutionException("Parameter 'baseUrl' is required");
    }
    if (StringUtils.isBlank(token)) {
      throw new MojoExecutionException("Parameter 'token' is required");
    }
  }
}
