package org.folio.app.generator.service;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
    var modulesLoadResult = moduleDescriptorService.loadModules(modules);
    var uiModulesLoadResult = moduleDescriptorService.loadModules(uiModules);

    return baseAppDescriptor
      .description(defaultIfBlank(template.getDescription(), mavenProject.getDescription()))
      .platform(defaultIfBlank(template.getPlatform(), "base"))
      .modules(modulesLoadResult.artifacts())
      .uiModules(uiModulesLoadResult.artifacts())
      .dependencies(emptyIfNull(template.getDependencies()))
      .moduleDescriptors(modulesLoadResult.descriptors())
      .uiModuleDescriptors(uiModulesLoadResult.descriptors());
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

  public static <T> List<T> emptyIfNull(List<T> list) {
    return list == null ? emptyList() : list;
  }

  public static String defaultIfBlank(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value;
  }

  private String getVersionWithBuildNumber(String version) {
    var buildNumber = pluginParameters.getBuildNumber();
    return version.endsWith("SNAPSHOT") && isNotBlank(buildNumber) ? version + "." + buildNumber : version;
  }
}
