package org.folio.app.generator.service;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.folio.app.generator.model.types.ModuleType.BE;
import static org.folio.app.generator.model.types.ModuleType.UI;
import static org.folio.app.generator.utils.PluginUtils.emptyIfNull;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.ApplicationDescriptor;
import org.folio.app.generator.model.ApplicationDescriptorTemplate;
import org.folio.app.generator.model.Dependency;
import org.folio.app.generator.model.ModuleDefinition;
import org.folio.app.generator.utils.PluginConfig;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationDescriptorService {
  private final MavenProject mavenProject;
  private final PluginConfig pluginParameters;
  private final ModuleDescriptorService moduleDescriptorService;

  /**
   * Creates {@link ApplicationDescriptor} based on template and project metadata.
   *
   * @param template - application descriptor template {@link ApplicationDescriptorTemplate}
   * @return created {@link ApplicationDescriptor} with all fields.
   */
  public ApplicationDescriptor create(ApplicationDescriptorTemplate template) throws MojoExecutionException {
    var name = template.getName();
    var version = template.getVersion();
    var baseAppDescriptor = name == null && version == null ? buildDescriptor() : buildDescriptor(template);

    var modules = convertToArtifacts(template.getModules());
    var uiModules = convertToArtifacts(template.getUiModules());
    var modulesLoadResult = moduleDescriptorService.loadModules(BE, modules);
    var uiModulesLoadResult = moduleDescriptorService.loadModules(UI, uiModules);

    baseAppDescriptor
      .description(defaultIfBlank(template.getDescription(), mavenProject.getDescription()))
      .modules(modulesLoadResult.artifacts())
      .uiModules(uiModulesLoadResult.artifacts())
      .dependencies(emptyIfNull(template.getDependencies()));

    if (!pluginParameters.isModuleUrlsOnly()) {
      baseAppDescriptor
        .moduleDescriptors(modulesLoadResult.descriptors())
        .uiModuleDescriptors(uiModulesLoadResult.descriptors());
      emptyIfNull(baseAppDescriptor.getModules())
        .forEach(md -> md.setUrl(null));
      emptyIfNull(baseAppDescriptor.getUiModules())
        .forEach(md -> md.setUrl(null));
    }
    return baseAppDescriptor;
  }

  private ApplicationDescriptor buildDescriptor() {
    var applicationDescriptor = new ApplicationDescriptor();
    var name = mavenProject.getName();
    var version = getVersionWithBuildNumber(mavenProject.getVersion());

    applicationDescriptor.setId(name + "-" + version);
    applicationDescriptor.setName(name);
    applicationDescriptor.setVersion(version);

    return applicationDescriptor;
  }

  private ApplicationDescriptor buildDescriptor(ApplicationDescriptorTemplate template) throws MojoExecutionException {
    var name = template.getName();
    var version = getVersionWithBuildNumber(template.getVersion());

    var applicationDescriptor = new ApplicationDescriptor();
    applicationDescriptor.setName(name);
    applicationDescriptor.setVersion(version);

    var generatedId = name + "-" + template.getVersion();
    if (template.getId() != null && !generatedId.equals(template.getId())) {
      throw new MojoExecutionException("Invalid application id provided in template");
    }

    applicationDescriptor.setId(name + "-" + version);
    return applicationDescriptor;
  }

  private List<ModuleDefinition> convertToArtifacts(List<Dependency> dependencies) {
    return emptyIfNull(dependencies).stream()
      .map(this::toArtifact)
      .toList();
  }

  private ModuleDefinition toArtifact(Dependency dependency) {
    var name = dependency.getName();
    var version = dependency.getVersion();
    return new ModuleDefinition().id(name + "-" + version).name(name).version(version);
  }

  private String getVersionWithBuildNumber(String version) {
    var buildNumber = pluginParameters.getBuildNumber();
    return version.endsWith("SNAPSHOT") && isNotBlank(buildNumber) ? version + "." + buildNumber : version;
  }
}
