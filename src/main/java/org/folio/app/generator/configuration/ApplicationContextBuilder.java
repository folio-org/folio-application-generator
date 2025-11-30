package org.folio.app.generator.configuration;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.registry.ModuleRegistries;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.model.types.ArtifactRegistryType;
import org.folio.app.generator.utils.PluginConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

public class ApplicationContextBuilder {

  private Log log;
  private PluginConfig pluginConfig;
  private MavenProject mavenProject;
  private MavenSession mavenSession;
  private ModuleRegistries moduleRegistries;

  public GenericApplicationContext build() {
    var context = new AnnotationConfigApplicationContext();
    setSpringContextProperties(context);

    context.registerBean("mavenLogger", Log.class, () -> log);
    context.registerBean("mavenProject", MavenProject.class, () -> mavenProject);
    context.registerBean("mavenSession", MavenSession.class, () -> mavenSession);
    context.registerBean("pluginConfig", PluginConfig.class, () -> pluginConfig);
    context.registerBean("moduleRegistries", ModuleRegistries.class, () -> moduleRegistries);
    context.registerBean(SpringConfiguration.class);

    context.refresh();

    return context;
  }

  /**
   * Sets log field and returns {@link ApplicationContextBuilder}.
   *
   * @return modified {@link ApplicationContextBuilder} value
   */
  public ApplicationContextBuilder withLog(Log log) {
    this.log = log;
    return this;
  }

  /**
   * Sets config field and returns {@link ApplicationContextBuilder}.
   *
   * @return modified {@link ApplicationContextBuilder} value
   */
  public ApplicationContextBuilder withPluginConfig(PluginConfig config) {
    this.pluginConfig = config;
    return this;
  }

  /**
   * Sets mavenProject field and returns {@link ApplicationContextBuilder}.
   *
   * @return modified {@link ApplicationContextBuilder} value
   */
  public ApplicationContextBuilder withMavenProject(MavenProject mavenProject) {
    this.mavenProject = mavenProject;
    return this;
  }

  /**
   * Sets mavenSession field and returns {@link ApplicationContextBuilder}.
   *
   * @return modified {@link ApplicationContextBuilder} value
   */
  public ApplicationContextBuilder withMavenSession(MavenSession mavenSession) {
    this.mavenSession = mavenSession;
    return this;
  }

  public ApplicationContextBuilder withModuleRegistries(ModuleRegistries moduleRegistries) {
    this.moduleRegistries = moduleRegistries;
    return this;
  }

  void setSpringContextProperties(GenericApplicationContext context) {
    var usedRegistryTypes = Stream.of(moduleRegistries.beRegistries(), moduleRegistries.uiRegistries())
      .filter(Objects::nonNull)
      .flatMap(Collection::stream)
      .map(ModuleRegistry::getType)
      .distinct()
      .toList();

    var springEnvironment = context.getEnvironment().getSystemProperties();
    for (var usedRegistryType : usedRegistryTypes) {
      springEnvironment.put(usedRegistryType.getPropertyName(), true);
    }

    if (pluginConfig.isValidateArtifacts()) {
      for (var artifactRegistryType : ArtifactRegistryType.values()) {
        springEnvironment.put(artifactRegistryType.getPropertyName(), true);
      }
    }
  }
}
