package org.folio.app.generator.configuration;

import static java.util.Collections.unmodifiableList;

import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.folio.app.generator.model.registry.ModuleRegistries;
import org.folio.app.generator.model.registry.ModuleRegistry;
import org.folio.app.generator.utils.PluginConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

public class ApplicationContextBuilder {

  private Log log;
  private PluginConfig pluginConfig;
  private MavenProject mavenProject;
  private MavenSession mavenSession;
  private List<ModuleRegistry> moduleRegistries;

  public GenericApplicationContext build() {
    var context = new AnnotationConfigApplicationContext();
    setSpringContextProperties(context);

    context.registerBean("mavenLogger", Log.class, () -> log);
    context.registerBean("mavenProject", MavenProject.class, () -> mavenProject);
    context.registerBean("mavenSession", MavenSession.class, () -> mavenSession);
    context.registerBean("pluginConfig", PluginConfig.class, () -> pluginConfig);
    context.registerBean("moduleRegistries", ModuleRegistries.class, () -> new ModuleRegistries(moduleRegistries));
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

  public ApplicationContextBuilder withModuleRegistries(List<ModuleRegistry> moduleRegistries) {
    this.moduleRegistries = unmodifiableList(moduleRegistries);
    return this;
  }

  private void setSpringContextProperties(GenericApplicationContext context) {
    var usedRegistryTypes = moduleRegistries.stream().map(ModuleRegistry::getType).distinct().toList();
    var springEnvironment = context.getEnvironment().getSystemProperties();
    for (var usedRegistryType : usedRegistryTypes) {
      springEnvironment.put(usedRegistryType.getPropertyName(), true);
    }
  }
}
